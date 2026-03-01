package com.example.informate;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String COLLECTION = "informate_stacks";
    private static final int CHUNK_MAX_CHARS = 2000;

    private String qdrantUrl;

    public EmbeddingService() {
        this.qdrantUrl = EnvLoader.getEnv("QDRANT_URL", "http://qdrant.iacob.uk:6333");
        initCollection();
    }

    private void initCollection() {
        try {
            // Check if collection exists
            int code = qdrantRequest("GET", "/collections/" + COLLECTION, null).getInt("status");
            if (code == 200) {
                logger.info("Qdrant collection '{}' already exists", COLLECTION);
                return;
            }
        } catch (Exception e) {
            // Collection doesn't exist, create it
        }
        try {
            JSONObject vectors = new JSONObject()
                .put("size", 768)
                .put("distance", "Cosine");
            JSONObject body = new JSONObject().put("vectors", vectors);
            qdrantRequest("PUT", "/collections/" + COLLECTION, body.toString());
            logger.info("Created Qdrant collection '{}'", COLLECTION);
        } catch (Exception e) {
            logger.error("Failed to create Qdrant collection", e);
        }
    }

    private JSONObject qdrantRequest(String method, String path, String body) throws IOException {
        URL url = new URL(qdrantUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = "";
        if (is != null) {
            try (Scanner s = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                response = s.hasNext() ? s.next() : "";
            }
        }

        JSONObject result = new JSONObject();
        result.put("status", code);
        if (!response.isEmpty()) {
            try {
                result.put("body", new JSONObject(response));
            } catch (JSONException e) {
                result.put("body", response);
            }
        }
        return result;
    }

    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > CHUNK_MAX_CHARS && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(sentence).append(" ");
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    public double[] embed(String text) {
        try {
            String ollamaUrl = EnvLoader.getEnv("OLLAMA_URL", "http://ollama.ai.svc.cluster.local:11434");
            String model = EnvLoader.getEnv("EMBEDDING_MODEL", "nomic-embed-text");

            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("prompt", text);

            URL url = new URL(ollamaUrl + "/api/embeddings");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                logger.error("Ollama embedding failed with status: {}", conn.getResponseCode());
                return null;
            }

            String response;
            try (Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                response = s.hasNext() ? s.next() : "";
            }

            JSONObject json = new JSONObject(response);
            JSONArray embedding = json.getJSONArray("embedding");
            double[] result = new double[embedding.length()];
            for (int i = 0; i < embedding.length(); i++) {
                result[i] = embedding.getDouble(i);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error generating embedding", e);
            return null;
        }
    }

    public void storeChunks(int stackId, int articleId, String articleTitle, String source, List<String> chunks) {
        JSONArray points = new JSONArray();
        for (int i = 0; i < chunks.size(); i++) {
            double[] vector = embed(chunks.get(i));
            if (vector == null) {
                logger.warn("Skipping chunk {} for article {} - embedding failed", i, articleId);
                continue;
            }

            String pointIdStr = "stack:" + stackId + ":article:" + articleId + ":chunk:" + i;
            UUID pointId = UUID.nameUUIDFromBytes(pointIdStr.getBytes(StandardCharsets.UTF_8));

            JSONObject payload = new JSONObject()
                .put("stack_id", stackId)
                .put("article_id", articleId)
                .put("article_title", articleTitle)
                .put("source", source)
                .put("chunk_index", i)
                .put("text", chunks.get(i));

            JSONArray vectorArr = new JSONArray();
            for (double v : vector) vectorArr.put(v);

            JSONObject point = new JSONObject()
                .put("id", pointId.toString())
                .put("vector", vectorArr)
                .put("payload", payload);
            points.put(point);
        }

        if (points.length() == 0) return;

        try {
            JSONObject body = new JSONObject().put("points", points);
            qdrantRequest("PUT", "/collections/" + COLLECTION + "/points", body.toString());
            logger.info("Stored {} chunks for article {} in stack {}", points.length(), articleId, stackId);
        } catch (Exception e) {
            logger.error("Error storing chunks in Qdrant", e);
        }
    }

    public List<Map<String, Object>> searchSimilar(int stackId, String queryText, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        double[] queryVector = embed(queryText);
        if (queryVector == null) return results;

        try {
            JSONArray vectorArr = new JSONArray();
            for (double v : queryVector) vectorArr.put(v);

            JSONObject filter = new JSONObject().put("must", new JSONArray()
                .put(new JSONObject().put("key", "stack_id")
                    .put("match", new JSONObject().put("value", stackId))));

            JSONObject body = new JSONObject()
                .put("vector", vectorArr)
                .put("limit", limit)
                .put("filter", filter)
                .put("with_payload", true);

            JSONObject response = qdrantRequest("POST",
                "/collections/" + COLLECTION + "/points/search", body.toString());

            if (response.getInt("status") != 200) return results;

            JSONArray resultArr = response.getJSONObject("body").getJSONArray("result");
            for (int i = 0; i < resultArr.length(); i++) {
                JSONObject r = resultArr.getJSONObject(i);
                JSONObject payload = r.getJSONObject("payload");
                Map<String, Object> chunk = new HashMap<>();
                chunk.put("text", payload.getString("text"));
                chunk.put("articleTitle", payload.getString("article_title"));
                chunk.put("source", payload.getString("source"));
                chunk.put("score", r.getDouble("score"));
                results.add(chunk);
            }
        } catch (Exception e) {
            logger.error("Error searching Qdrant for stack: {}", stackId, e);
        }
        return results;
    }

    public void deleteByStackId(int stackId) {
        try {
            JSONObject filter = new JSONObject().put("must", new JSONArray()
                .put(new JSONObject().put("key", "stack_id")
                    .put("match", new JSONObject().put("value", stackId))));
            JSONObject body = new JSONObject().put("filter", filter);
            qdrantRequest("POST",
                "/collections/" + COLLECTION + "/points/delete", body.toString());
            logger.info("Deleted vectors for stack: {}", stackId);
        } catch (Exception e) {
            logger.error("Error deleting vectors for stack: {}", stackId, e);
        }
    }
}
