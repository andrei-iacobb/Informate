package com.example.informate;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);
    private EmbeddingService embeddingService;

    public AnalysisService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public String generateAnalysis(int stackId, String keywords, String focus, int articleCount) {
        // Step 1: Retrieve relevant chunks from Qdrant
        String queryText = keywords;
        if (focus != null && !focus.isEmpty()) {
            queryText = keywords + " " + focus;
        }

        List<Map<String, Object>> chunks = embeddingService.searchSimilar(stackId, queryText, 50);
        if (chunks.isEmpty()) {
            logger.warn("No chunks found for stack: {}", stackId);
            return null;
        }

        // Step 2: Build source material from chunks
        StringBuilder sourceMaterial = new StringBuilder();
        Set<String> seenTitles = new HashSet<>();
        for (Map<String, Object> chunk : chunks) {
            String title = (String) chunk.get("articleTitle");
            String text = (String) chunk.get("text");
            String source = (String) chunk.get("source");

            if (!seenTitles.contains(title)) {
                sourceMaterial.append("\n### Source: ").append(title)
                    .append(" [").append(source).append("]\n");
                seenTitles.add(title);
            }
            sourceMaterial.append(text).append("\n");
        }

        // Step 3: Build focus instruction
        String focusInstruction;
        if (focus != null && !focus.isEmpty()) {
            focusInstruction = "Pay particular attention to: " + focus +
                ". Prioritize analysis of this aspect while still covering other dimensions.";
        } else {
            focusInstruction = "Provide a comprehensive analysis covering all aspects " +
                "including geopolitical, economic, social, and technological dimensions.";
        }

        // Step 4: Build the analysis prompt
        String prompt = "You are an expert intelligence analyst. Using the following source material\n" +
            "from " + articleCount + " news articles, produce a structured briefing report.\n\n" +
            focusInstruction + "\n\n" +
            "## Source Material\n" + sourceMaterial + "\n\n" +
            "## Report Format (respond in valid JSON only, no markdown fences)\n\n" +
            "{\n" +
            "  \"executive_summary\": \"2-3 paragraphs covering the core situation...\",\n" +
            "  \"key_facts\": [\"fact 1\", \"fact 2\"],\n" +
            "  \"perspectives\": [\n" +
            "    {\"viewpoint\": \"...\", \"description\": \"...\"}\n" +
            "  ],\n" +
            "  \"market_impact\": {\n" +
            "    \"summary\": \"...\",\n" +
            "    \"sectors\": [\"...\"],\n" +
            "    \"tickers\": [\"...\"],\n" +
            "    \"outlook\": \"...\"\n" +
            "  },\n" +
            "  \"future_scenarios\": [\n" +
            "    {\n" +
            "      \"scenario\": \"...\",\n" +
            "      \"probability\": \"Low|Medium|High\",\n" +
            "      \"reasoning\": \"...\",\n" +
            "      \"indicators\": [\"what to watch for\"],\n" +
            "      \"timeline\": \"...\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"sources\": [\n" +
            "    {\"title\": \"...\", \"url\": \"...\"}\n" +
            "  ]\n" +
            "}";

        // Step 5: Call Ollama LLM
        String response = callLLM(prompt);
        if (response == null) return null;

        // Step 6: Extract JSON from response
        return extractJson(response);
    }

    private String callLLM(String prompt) {
        try {
            String ollamaUrl = EnvLoader.getEnv("OLLAMA_URL", "http://ollama.ai.svc.cluster.local:11434");
            String model = EnvLoader.getEnv("OLLAMA_MODEL", "llama3.1:70b");

            JSONObject message = new JSONObject()
                .put("role", "user")
                .put("content", prompt);
            JSONObject body = new JSONObject()
                .put("model", model)
                .put("messages", new JSONArray().put(message));

            URL url = new URL(ollamaUrl + "/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(300000); // 5 min for large analysis

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                logger.error("Ollama analysis call failed with status: {}", conn.getResponseCode());
                return null;
            }

            String response;
            try (Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                response = s.hasNext() ? s.next() : "";
            }

            JSONObject json = new JSONObject(response);
            return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
        } catch (Exception e) {
            logger.error("Error calling Ollama for analysis", e);
            return null;
        }
    }

    private String extractJson(String content) {
        // Try to find JSON in the response (may be wrapped in markdown fences)
        String trimmed = content.trim();

        // Remove markdown code fences if present
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // Find the first { and last }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String jsonStr = trimmed.substring(start, end + 1);
            // Validate it's valid JSON
            try {
                new JSONObject(jsonStr);
                return jsonStr;
            } catch (JSONException e) {
                logger.error("Invalid JSON in analysis response");
            }
        }
        logger.error("Could not extract JSON from analysis response");
        return null;
    }

    public String suggestStackName(String keywords) {
        try {
            String prompt = "Given these news article keywords: " + keywords +
                "\n\nSuggest a concise 3-6 word topic name for a news analysis stack. " +
                "Reply with just the name, nothing else. No quotes.";
            String response = callLLM(prompt);
            if (response != null) {
                return response.trim().replaceAll("^\"|\"$", "");
            }
        } catch (Exception e) {
            logger.error("Error suggesting stack name", e);
        }
        return "New Stack";
    }
}
