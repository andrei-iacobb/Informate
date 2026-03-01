package com.example.informate;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    public boolean isAvailable() {
        String url = EnvLoader.getEnv("SEARXNG_URL", "");
        return url != null && !url.isEmpty();
    }

    public List<Map<String, String>> search(String keywords, int maxResults) {
        List<Map<String, String>> results = new ArrayList<>();
        String baseUrl = EnvLoader.getEnv("SEARXNG_URL", "");
        if (baseUrl == null || baseUrl.isEmpty()) {
            logger.info("SearXNG not configured, skipping search");
            return results;
        }

        try {
            String encoded = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
            String requestUrl = baseUrl + "/search?q=" + encoded + "&categories=news&format=json";

            HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                logger.error("SearXNG returned status: {}", conn.getResponseCode());
                return results;
            }

            String body;
            try (Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                body = s.hasNext() ? s.next() : "";
            }

            JSONObject response = new JSONObject(body);
            JSONArray resultArray = response.getJSONArray("results");

            Set<String> seenUrls = new HashSet<>();
            for (int i = 0; i < resultArray.length() && results.size() < maxResults; i++) {
                JSONObject r = resultArray.getJSONObject(i);
                String url = r.optString("url", "");
                if (url.isEmpty() || seenUrls.contains(url)) continue;
                seenUrls.add(url);

                Map<String, String> result = new HashMap<>();
                result.put("url", url);
                result.put("title", r.optString("title", ""));
                result.put("snippet", r.optString("content", ""));
                result.put("engine", r.optString("engine", ""));
                results.add(result);
            }

            logger.info("SearXNG returned {} unique results for: {}", results.size(), keywords);
        } catch (Exception e) {
            logger.error("Error searching SearXNG for: {}", keywords, e);
        }
        return results;
    }
}
