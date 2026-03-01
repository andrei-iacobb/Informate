/**
 * MOD003484
 * SID: 2402513
 * Project: Informate
 * Author: Andrei
 * 
 * Main REST API server for the Informate Backend.
 * This class serves as the entry point for the web API and manages:
 * - User authentication endpoints
 * - Article management endpoints
 * - Real-time WebSocket updates for article processing
 * - CORS configuration for frontend integration
 */

package com.example.informate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.delete;
import static spark.Spark.staticFiles;

/**
 * Main REST API server class for the Informate Backend.
 * Provides RESTful endpoints for authentication, article management,
 * and real-time updates for article processing.
 */
public class main {
    // Service instances
    static scraper scrap = new scraper();
    static auth au = new auth();
    static articles art = new articles();
    static AI ai = new AI();
    static StackService stackService = new StackService();
    static SearchService searchService = new SearchService();
    static EmbeddingService embeddingService = new EmbeddingService();
    static AnalysisService analysisService = new AnalysisService(embeddingService);
    static Gson gson = new Gson();
    
    // Store for real-time processing updates
    static Map<String, String> processingStatus = new ConcurrentHashMap<>();

    /**
     * Application entry point. Initializes the REST API server with all endpoints.
     * Sets up CORS, authentication endpoints, article endpoints, and WebSocket support.
     *
     * @param args Command line arguments (not used in current implementation)
     */
    public static void main(String[] args) {
        System.out.println("Starting Informate REST API Server");

        // Initialize the authentication database
        au.initialiseDB();

        // Configure server port
        port(8080);

        // Serve static image files from /app/SiteImages
        staticFiles.externalLocation("/app/SiteImages");

        // Enable CORS for frontend integration
        enableCORS();

        // Setup API routes
        setupAuthRoutes();
        setupArticleRoutes();
        setupStackRoutes();

        // Start server
        System.out.println("Informate API Server running on http://localhost:8080");
    }

    /**
     * Enables CORS for all routes to allow frontend requests.
     */
    private static void enableCORS() {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
            response.type("application/json");
        });
    }

    /**
     * Sets up authentication-related API endpoints.
     * Includes login, register, and token validation routes.
     */
    private static void setupAuthRoutes() {
        // Login endpoint
        post("/api/auth/login", (req, res) -> {
            JsonObject loginData = gson.fromJson(req.body(), JsonObject.class);
            String username = loginData.get("username").getAsString();
            String password = loginData.get("password").getAsString();
            
            // Validate credentials using auth service
            String token = au.loginWithPassword(username, password);
            
            JsonObject response = new JsonObject();
            if (token != null && au.isTokenValid(token)) {
                response.addProperty("success", true);
                response.addProperty("token", token);
                response.addProperty("message", "Login successful");
            } else {
                res.status(401);
                response.addProperty("success", false);
                response.addProperty("message", "Invalid credentials");
            }
            
            return gson.toJson(response);
        });

        // Register endpoint
        post("/api/auth/register", (req, res) -> {
            System.out.println("=== REGISTRATION REQUEST ===");
            System.out.println("Request body: " + req.body());
            
            JsonObject registerData = gson.fromJson(req.body(), JsonObject.class);
            String username = registerData.get("username").getAsString();
            String password = registerData.get("password").getAsString();
            
            System.out.println("Attempting to register user: " + username);
            System.out.println("Password length: " + password.length());
            
            JsonObject response = new JsonObject();
            try {
                au.insertDetails(username, password);
                System.out.println("Registration successful for user: " + username);
                response.addProperty("success", true);
                response.addProperty("message", "Registration successful");
            } catch (Exception e) {
                System.err.println("Registration failed for user: " + username);
                System.err.println("Error details: " + e.getMessage());
                e.printStackTrace();
                res.status(400);
                response.addProperty("success", false);
                response.addProperty("message", "Registration failed: " + e.getMessage());
            }
            
            System.out.println("Response: " + gson.toJson(response));
            System.out.println("=== END REGISTRATION REQUEST ===");
            return gson.toJson(response);
        });

        // Token validation endpoint
        get("/api/auth/validate", (req, res) -> {
            String token = req.headers("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            JsonObject response = new JsonObject();
            if (au.isTokenValid(token)) {
                response.addProperty("valid", true);
                response.addProperty("username", au.getUsernameFromToken(token));
            } else {
                response.addProperty("valid", false);
            }
            
            return gson.toJson(response);
        });
    }

    /**
     * Sets up article-related API endpoints.
     * Includes routes for getting articles, adding new articles, and processing status.
     */
    private static void setupArticleRoutes() {
        // Get all articles
        get("/api/articles", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            
            List<Map<String, String>> articles = art.getAllArticles();
            return gson.toJson(Map.of("articles", articles));
        });

        // Get specific article by title
        get("/api/articles/:title", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            
            String title = req.params(":title");
            Map<String, String> article = art.getArticleByTitle(title);
            
            if (article.isEmpty()) {
                res.status(404);
                return gson.toJson(Map.of("error", "Article not found"));
            }
            
            return gson.toJson(article);
        });

        // Add new article
        post("/api/articles", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            
            JsonObject articleData = gson.fromJson(req.body(), JsonObject.class);
            String url = articleData.get("url").getAsString();
            
            // Generate processing ID for real-time updates
            String processingId = UUID.randomUUID().toString();
            
            // Start async processing
            CompletableFuture.runAsync(() -> processArticleAsync(url, processingId));
            
            return gson.toJson(Map.of(
                "processingId", processingId,
                "message", "Article processing started"
            ));
        });

        // Get processing status
        get("/api/articles/status/:processingId", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }

            String processingId = req.params(":processingId");
            String status = processingStatus.getOrDefault(processingId, "unknown");

            return gson.toJson(Map.of("status", status));
        });
    }

    /**
     * Sets up stack-related API endpoints.
     * Includes routes for CRUD operations on stacks and stack-article associations.
     */
    private static void setupStackRoutes() {
        // List all stacks
        get("/api/stacks", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            return gson.toJson(Map.of("stacks", stackService.getAllStacks()));
        });

        // Create new stack
        post("/api/stacks", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            JsonObject data = gson.fromJson(req.body(), JsonObject.class);
            String name = data.get("name").getAsString();
            String keywords = data.has("keywords") ? data.get("keywords").getAsString() : "";
            String focus = data.has("focus") && !data.get("focus").isJsonNull() ? data.get("focus").getAsString() : null;
            int searchDepth = data.has("searchDepth") ? data.get("searchDepth").getAsInt() : 10;

            int id = stackService.createStack(name, keywords, focus, searchDepth);
            if (id > 0) {
                return gson.toJson(Map.of("success", true, "id", id));
            }
            res.status(500);
            return gson.toJson(Map.of("error", "Failed to create stack"));
        });

        // Get stack detail with articles
        get("/api/stacks/:id", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            int id = Integer.parseInt(req.params(":id"));
            Map<String, Object> stack = stackService.getStack(id);
            if (stack.isEmpty()) {
                res.status(404);
                return gson.toJson(Map.of("error", "Stack not found"));
            }
            stack.put("articles", stackService.getStackArticles(id));
            return gson.toJson(stack);
        });

        // Update stack
        put("/api/stacks/:id", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            int id = Integer.parseInt(req.params(":id"));
            JsonObject data = gson.fromJson(req.body(), JsonObject.class);
            String focus = data.has("focus") && !data.get("focus").isJsonNull() ? data.get("focus").getAsString() : null;
            int searchDepth = data.has("searchDepth") ? data.get("searchDepth").getAsInt() : 10;
            stackService.updateStack(id, focus, searchDepth);
            return gson.toJson(Map.of("success", true));
        });

        // Delete stack
        delete("/api/stacks/:id", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            int id = Integer.parseInt(req.params(":id"));
            stackService.deleteStack(id);
            return gson.toJson(Map.of("success", true));
        });

        // Suggest stack for an article
        post("/api/stacks/suggest", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            JsonObject data = gson.fromJson(req.body(), JsonObject.class);

            String keywords = "";
            if (data.has("articleId")) {
                Map<String, String> article = art.getArticleById(data.get("articleId").getAsInt());
                keywords = article.getOrDefault("keywords", "");
            } else if (data.has("keywords")) {
                keywords = data.get("keywords").getAsString();
            }

            if (keywords.isEmpty()) {
                return gson.toJson(Map.of("existingStacks", List.of(), "suggestedName", "New Stack"));
            }

            Set<String> articleKeywords = new HashSet<>(
                Arrays.asList(keywords.toLowerCase().split("\\s*,\\s*")));

            List<Map<String, Object>> allStacks = stackService.getAllStacks();
            List<Map<String, Object>> matches = new ArrayList<>();

            for (Map<String, Object> stack : allStacks) {
                String stackKw = (String) stack.get("keywords");
                if (stackKw == null || stackKw.isEmpty()) continue;

                Set<String> stackKeywords = new HashSet<>(
                    Arrays.asList(stackKw.toLowerCase().split("\\s*,\\s*")));
                long overlap = articleKeywords.stream().filter(stackKeywords::contains).count();
                if (overlap > 0) {
                    double score = (double) overlap / Math.max(articleKeywords.size(), 1);
                    Map<String, Object> match = new HashMap<>();
                    match.put("id", stack.get("id"));
                    match.put("name", stack.get("name"));
                    match.put("matchScore", Math.round(score * 100));
                    matches.add(match);
                }
            }

            matches.sort((a, b) -> Long.compare(
                (long) b.get("matchScore"), (long) a.get("matchScore")));

            String suggestedName = analysisService.suggestStackName(keywords);

            Map<String, Object> result = new HashMap<>();
            result.put("existingStacks", matches);
            result.put("suggestedName", suggestedName);
            return gson.toJson(result);
        });

        // Add article to stack
        post("/api/stacks/:id/articles", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            int stackId = Integer.parseInt(req.params(":id"));
            JsonObject data = gson.fromJson(req.body(), JsonObject.class);
            int articleId = data.get("articleId").getAsInt();
            String source = data.has("source") ? data.get("source").getAsString() : "manual";
            stackService.addArticleToStack(stackId, articleId, source);
            return gson.toJson(Map.of("success", true));
        });

        // Remove article from stack
        delete("/api/stacks/:id/articles/:articleId", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            int stackId = Integer.parseInt(req.params(":id"));
            int articleId = Integer.parseInt(req.params(":articleId"));
            stackService.removeArticleFromStack(stackId, articleId);
            return gson.toJson(Map.of("success", true));
        });

        // Trigger analysis pipeline
        post("/api/stacks/:id/analyze", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            int stackId = Integer.parseInt(req.params(":id"));
            Map<String, Object> stack = stackService.getStack(stackId);
            if (stack.isEmpty()) {
                res.status(404);
                return gson.toJson(Map.of("error", "Stack not found"));
            }

            String currentStatus = (String) stack.get("status");
            if ("searching".equals(currentStatus) || "analyzing".equals(currentStatus)) {
                return gson.toJson(Map.of("message", "Pipeline already running"));
            }

            CompletableFuture.runAsync(() -> runStackPipeline(stackId));
            return gson.toJson(Map.of("success", true, "message", "Analysis started"));
        });

        // Get stack pipeline status
        get("/api/stacks/:id/status", (req, res) -> {
            String token = extractToken(req.headers("Authorization"));
            if (!au.isTokenValid(token)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Unauthorized"));
            }
            int id = Integer.parseInt(req.params(":id"));
            Map<String, Object> stack = stackService.getStack(id);
            if (stack.isEmpty()) {
                res.status(404);
                return gson.toJson(Map.of("error", "Stack not found"));
            }
            return gson.toJson(Map.of("status", stack.get("status")));
        });
    }

    /**
     * Processes an article asynchronously and updates the processing status.
     *
     * @param url The URL of the article to process
     * @param processingId The unique ID for tracking processing status
     */
    private static void processArticleAsync(String url, String processingId) {
        try {
            processingStatus.put(processingId, "Scraping website...");
            
            // Get the article title
            String title = scrap.scrapeForTitle(url);
            if (title == null || title.trim().isEmpty()) {
                processingStatus.put(processingId, "Error: Could not find article title");
                return;
            }
            
            processingStatus.put(processingId, "Extracting content...");
            
            // Get the article text
            String articleText = scrap.scrapePageForText(url);
            if (articleText == null || articleText.trim().isEmpty()) {
                processingStatus.put(processingId, "Error: Could not extract article text");
                return;
            }
            
            processingStatus.put(processingId, "Processing images...");
            
            // Get images if available
            List<String> images = scrap.scrapePageForImages(url, title);
            
            processingStatus.put(processingId, "Saving to database...");
            
            // Save the raw text
            art.insertRawText(title, articleText);
            
            processingStatus.put(processingId, "Processing with AI...");
            
            // Process with AI
            AI.processArticle(articleText, title, images);
            
            processingStatus.put(processingId, "Complete");
            
        } catch (Exception e) {
            processingStatus.put(processingId, "Error: " + e.getMessage());
        }
    }

    /**
     * Runs the full stack analysis pipeline: search, embed, and analyze.
     *
     * @param stackId The ID of the stack to process
     */
    private static void runStackPipeline(int stackId) {
        try {
            stackService.updateStatus(stackId, "searching");

            Map<String, Object> stack = stackService.getStack(stackId);
            String keywords = (String) stack.get("keywords");
            String focus = (String) stack.get("focus");
            int searchDepth = (int) stack.get("searchDepth");

            // Step 1: Search for additional articles via SearXNG
            if (searchService.isAvailable() && keywords != null && !keywords.isEmpty()) {
                List<Map<String, String>> searchResults = searchService.search(keywords, searchDepth);
                for (Map<String, String> result : searchResults) {
                    try {
                        String url = result.get("url");
                        String title = scrap.scrapeForTitle(url);
                        if (title == null || title.trim().isEmpty()) continue;

                        String text = scrap.scrapePageForText(url);
                        if (text == null || text.trim().isEmpty()) continue;

                        art.insertRawText(title, text);

                        Map<String, String> saved = art.getArticleByTitle(title);
                        if (saved.containsKey("id")) {
                            int articleId = Integer.parseInt(saved.get("id"));
                            stackService.addArticleToStack(stackId, articleId, "searxng");
                        }

                        Thread.sleep(1500);
                    } catch (Exception e) {
                        System.err.println("Error processing search result: " + e.getMessage());
                    }
                }
            }

            // Step 2: Embed all stack articles
            stackService.updateStatus(stackId, "analyzing");

            embeddingService.deleteByStackId(stackId);

            List<Map<String, Object>> allArticles = stackService.getStackArticles(stackId);
            Set<String> allKeywords = new HashSet<>();

            for (Map<String, Object> article : allArticles) {
                int articleId = (int) article.get("id");
                String title = (String) article.get("title");
                String source = (String) article.get("source");

                Map<String, String> fullArticle = art.getArticleById(articleId);
                String rawText = fullArticle.get("rawText");
                if (rawText == null || rawText.isEmpty()) continue;

                String artKeywords = fullArticle.get("keywords");
                if (artKeywords != null) {
                    Arrays.stream(artKeywords.split(","))
                        .map(String::trim)
                        .filter(k -> !k.isEmpty())
                        .forEach(allKeywords::add);
                }

                List<String> chunks = embeddingService.chunkText(rawText);
                embeddingService.storeChunks(stackId, articleId, title, source, chunks);
            }

            if (!allKeywords.isEmpty()) {
                stackService.updateKeywords(stackId, String.join(",", allKeywords));
            }

            // Step 3: Generate analysis
            stack = stackService.getStack(stackId);
            keywords = (String) stack.get("keywords");
            String analysis = analysisService.generateAnalysis(
                stackId, keywords, focus, allArticles.size());

            if (analysis != null) {
                stackService.updateAnalysis(stackId, analysis);
            } else {
                stackService.updateStatus(stackId, "error");
            }
        } catch (Exception e) {
            System.err.println("Pipeline error for stack " + stackId + ": " + e.getMessage());
            stackService.updateStatus(stackId, "error");
        }
    }

    /**
     * Extracts the token from the Authorization header.
     *
     * @param authHeader The Authorization header value
     * @return The extracted token or null if invalid format
     */
    private static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
