/**
 * MOD003484
 * SID: 2402513
 * Project: Informate
 * Author: Andrei
 */

package com.example.informate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles interactions with a configurable LLM (Ollama) to process article text.
 * This class is responsible for:
 * - Preparing text by cleaning and formatting
 * - Communicating with the LLM API for text summarization and keyword extraction
 * - Processing and parsing the API responses
 * - Managing article data storage
 */
public class AI {
    private static final Logger logger = LoggerFactory.getLogger(AI.class);

    /**
     * Returns the LLM API endpoint URL, configurable via OLLAMA_URL env var.
     */
    private static String getApiUrl() {
        return EnvLoader.getEnv("OLLAMA_URL", "http://ollama.ai.svc.cluster.local:11434") + "/v1/chat/completions";
    }

    /**
     * Returns the LLM model name, configurable via OLLAMA_MODEL env var.
     */
    private static String getModel() {
        return EnvLoader.getEnv("OLLAMA_MODEL", "llama3.1:70b");
    }

    /**
     * A simple data structure to hold the results returned by the AI processing.
     * Contains the generated summary and a list of extracted keywords.
     */
    public static class ArticleResult {
        /** The summarized text of the article. */
        public String summary;
        /** A list of keywords extracted from the article. */
        public List<String> keywords;
    }

    /**
     * Processes a given article text using the LLM API.
     * This method performs the following steps:
     * 1. Validates input parameters
     * 2. Prepares text by cleaning and formatting
     * 3. Calls the LLM API with the prepared text
     * 4. Parses the API response to extract summary and keywords
     * 5. Updates the article in the database with the processed data
     *
     * @param articleText The raw text content of the article. Must not be null or empty.
     * @param title The title of the article. Must not be null or empty.
     * @param imageFilenames A list of filenames for images associated with the article. May be null or empty.
     * @throws IllegalArgumentException if articleText or title is null or empty
     * @throws IOException if there's an error during text preparation or API communication
     */
    public static void processArticle(String articleText, String title, List<String> imageFilenames) 
            throws IllegalArgumentException, IOException {
        logger.debug("Starting article processing for title: {}", title);
        
        // Input validation
        if (articleText == null || articleText.trim().isEmpty()) {
            logger.error("Article text is null or empty for title: {}", title);
            throw new IllegalArgumentException("Article text cannot be null or empty");
        }
        if (title == null || title.trim().isEmpty()) {
            logger.error("Article title is null or empty");
            throw new IllegalArgumentException("Article title cannot be null or empty");
        }

        try {
            logger.debug("Preparing text for article: {}", title);
            String preparedText = prepareText(articleText);

            logger.debug("Calling LLM API for article: {}", title);
            String apiResponse = callLLM(preparedText);

            if (apiResponse == null) {
                logger.error("No response received from LLM API for article: {}", title);
                throw new IOException("No response received from LLM API");
            }

            logger.debug("Parsing AI response for article: {}", title);
            ArticleResult result = parseAIResponse(extractSummary(apiResponse));

            if (result != null) {
                logger.info("Successfully processed article: {}", title);
                articles art = new articles();
                art.insertCompleteArticle(title, result.summary, result.keywords, 
                    imageFilenames != null ? imageFilenames : List.of());
            } else {
                logger.error("Failed to parse AI response for article: {}", title);
                throw new IOException("Failed to parse AI response for article: " + title);
            }
        } catch (Exception e) {
            logger.error("Error processing article: {}", title, e);
            throw new IOException("Error processing article: " + e.getMessage(), e);
        }
    }

    /**
     * Prepares raw text for the AI by cleaning and formatting.
     * This method:
     * 1. Removes extra whitespace
     * 2. Splits text into sentences using simple rules
     * 3. Joins sentences with spaces
     *
     * @param rawText The original article text. Must not be null.
     * @return The cleaned and formatted text.
     * @throws IllegalArgumentException if rawText is null
     */
    private static String prepareText(String rawText) throws IOException {
        logger.debug("Preparing text for processing");
        if (rawText == null) {
            logger.error("Raw text is null");
            throw new IllegalArgumentException("Raw text cannot be null");
        }

        // Simple sentence splitting using regex
        Pattern pattern = Pattern.compile("(?<=[.!?])\\s+");
        String[] sentences = pattern.split(rawText.trim());

        // Clean each sentence and join them
        String cleanedText = Arrays.stream(sentences)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(java.util.stream.Collectors.joining(" "));

        logger.debug("Text prepared, length: {}", cleanedText.length());
        return cleanedText;
    }

    /**
     * Calls the LLM Chat Completions API with the provided text.
     * This method:
     * 1. Sets up the HTTP connection
     * 2. Constructs the request payload
     * 3. Sends the request and handles the response
     *
     * @param inputText The prepared text to send to the AI. Must not be null.
     * @return The JSON response string from the API, or null if an error occurred.
     * @throws IllegalArgumentException if inputText is null
     * @throws IOException if there's an error during API communication
     */
    private static String callLLM(String inputText) throws IOException {
        logger.debug("Calling LLM API");
        if (inputText == null) {
            logger.error("Input text is null");
            throw new IllegalArgumentException("Input text cannot be null");
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(getApiUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);  // 10s connect timeout
            conn.setReadTimeout(120000);    // 120s read timeout
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", generatePrompt(inputText));

            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject payload = new JSONObject();
            payload.put("model", getModel());
            payload.put("messages", messages);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            logger.debug("LLM API response status: {}", status);

            InputStream responseStream = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(responseStream, StandardCharsets.UTF_8.name())) {
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
            }

            return (status == 200) ? response.toString() : null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Generates the prompt to be sent to the LLM API.
     * The prompt instructs the AI to:
     * 1. Act as a news editor
     * 2. Summarize the article
     * 3. Extract keywords
     *
     * @param text The article text to be included in the prompt. Must not be null.
     * @return The constructed prompt string.
     * @throws IllegalArgumentException if text is null
     */
    private static String generatePrompt(String text) {
        logger.debug("Generating prompt for text processing");
        if (text == null) {
            logger.error("Text is null");
            throw new IllegalArgumentException("Text cannot be null");
        }

        return String.format(
            "You are a professional news editor. Your task is to:\n" +
            "1. Create a comprehensive 2-3 paragraph summary of the following news article that covers:\n" +
            "   - The main story and key facts\n" +
            "   - Important context and background\n" +
            "   - Significant implications or outcomes\n" +
            "   Make it detailed and informative while remaining clear and engaging.\n" +
            "2. Extract 5-10 relevant keywords\n\n" +
            "Format your response exactly like this:\n" +
            "Summary: <your detailed summary here>\n" +
            "Keywords: <comma-separated keywords>\n\n" +
            "Article text:\n%s", text);
    }

    /**
     * Extracts the main content from the LLM API's JSON response.
     *
     * @param jsonResponse The JSON response string from the API. Must not be null.
     * @return The content string from the first choice's message, or null if parsing fails.
     * @throws IllegalArgumentException if jsonResponse is null
     * @throws org.json.JSONException if the JSON structure is invalid
     */
    private static String extractSummary(String jsonResponse) {
        logger.debug("Extracting content from API response");
        if (jsonResponse == null) {
            logger.error("JSON response is null");
            throw new IllegalArgumentException("JSON response cannot be null");
        }

        try {
            JSONObject obj = new JSONObject(jsonResponse);
            JSONArray choices = obj.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.getJSONObject("message");
                return message.getString("content").trim();
            }
            logger.warn("No choices found in API response");
            return null;
        } catch (Exception e) {
            logger.error("Error parsing JSON response", e);
            return null;
        }
    }

    /**
     * Parses the AI-generated content string into an ArticleResult object.
     *
     * @param aiResponse The content string from the AI. Must not be null.
     * @return An ArticleResult containing the summary and keywords, or null if parsing fails.
     * @throws IllegalArgumentException if aiResponse is null
     */
    private static ArticleResult parseAIResponse(String aiResponse) {
        logger.debug("Parsing AI response");
        if (aiResponse == null) {
            logger.error("AI response is null");
            throw new IllegalArgumentException("AI response cannot be null");
        }

        try {
            ArticleResult result = new ArticleResult();

            // Look for Summary and Keywords sections
            String[] parts = aiResponse.split("\\n");
            StringBuilder summary = new StringBuilder();
            List<String> keywords = new ArrayList<>();
            boolean inSummary = false;

            for (String line : parts) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Summary:")) {
                    inSummary = true;
                    summary.append(line.substring("Summary:".length()).trim());
                } else if (line.startsWith("Keywords:")) {
                    inSummary = false;
                    String keywordText = line.substring("Keywords:".length()).trim();
                    keywords.addAll(Arrays.asList(keywordText.split("\\s*,\\s*")));
                } else if (inSummary) {
                    summary.append(" ").append(line);
                }
            }

            // Validate the parsed content
            if (summary.length() == 0 || keywords.isEmpty()) {
                logger.error("Failed to parse AI response. Content: {}", aiResponse);
                return null;
            }

            result.summary = summary.toString().trim();
            result.keywords = keywords.stream()
                .filter(k -> !k.isEmpty())
                .collect(java.util.stream.Collectors.toList());

            logger.debug("Successfully parsed AI response. Summary length: {}, Keywords count: {}", 
                result.summary.length(), result.keywords.size());
            return result;
        } catch (Exception e) {
            logger.error("Error parsing AI response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Logs exceptions to the error log file.
     *
     * @param e The exception to log. Must not be null.
     * @throws IllegalArgumentException if e is null
     */
    private static void logError(Exception e) {
        if (e == null) {
            throw new IllegalArgumentException("Exception cannot be null");
        }

        try (PrintWriter writer = new PrintWriter(new FileOutputStream("error.log", true))) {
            writer.println("----- AI Exception Logged: " + new java.util.Date() + " -----");
            e.printStackTrace(writer);
            writer.println("---------------------------------------------");
        } catch (IOException io) {
            System.err.println("CRITICAL: Failed to write to error log file.");
            e.printStackTrace();
        }
    }
}
