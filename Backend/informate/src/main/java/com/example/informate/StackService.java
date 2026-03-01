package com.example.informate;

import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackService {
    private static final Logger logger = LoggerFactory.getLogger(StackService.class);
    private Connection conn;

    public StackService() {
        try {
            String dbUrl = EnvLoader.getEnv("DATABASE_URL", "jdbc:postgresql://localhost:5432/informate");
            String dbUser = EnvLoader.getEnv("POSTGRES_USER", "informate");
            String dbPassword = EnvLoader.getEnv("POSTGRES_PASSWORD", "");
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS stacks (
                        id SERIAL PRIMARY KEY,
                        name TEXT NOT NULL,
                        keywords TEXT,
                        focus TEXT,
                        search_depth INTEGER DEFAULT 10,
                        status TEXT DEFAULT 'pending',
                        analysis TEXT,
                        created_at TIMESTAMP DEFAULT NOW(),
                        updated_at TIMESTAMP DEFAULT NOW()
                    )
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS stack_articles (
                        id SERIAL PRIMARY KEY,
                        stack_id INTEGER REFERENCES stacks(id) ON DELETE CASCADE,
                        article_id INTEGER REFERENCES articles(id),
                        source TEXT DEFAULT 'manual',
                        added_at TIMESTAMP DEFAULT NOW(),
                        UNIQUE(stack_id, article_id)
                    )
                """);
            }
            logger.info("Stack tables initialized successfully");
        } catch (SQLException e) {
            logger.error("FATAL: Failed to initialize stack tables", e);
            System.exit(1);
        }
    }

    public int createStack(String name, String keywords, String focus, int searchDepth) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO stacks (name, keywords, focus, search_depth) VALUES (?, ?, ?, ?) RETURNING id")) {
            ps.setString(1, name);
            ps.setString(2, keywords);
            ps.setString(3, focus);
            ps.setInt(4, Math.min(searchDepth, 30));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.error("Error creating stack: {}", name, e);
        }
        return -1;
    }

    public Map<String, Object> getStack(int id) {
        Map<String, Object> stack = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stacks WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stack.put("id", rs.getInt("id"));
                    stack.put("name", rs.getString("name"));
                    stack.put("keywords", rs.getString("keywords"));
                    stack.put("focus", rs.getString("focus"));
                    stack.put("searchDepth", rs.getInt("search_depth"));
                    stack.put("status", rs.getString("status"));
                    stack.put("analysis", rs.getString("analysis"));
                    stack.put("createdAt", rs.getString("created_at"));
                    stack.put("updatedAt", rs.getString("updated_at"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting stack: {}", id, e);
        }
        return stack;
    }

    public List<Map<String, Object>> getAllStacks() {
        List<Map<String, Object>> stacks = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT s.*, COUNT(sa.id) as article_count FROM stacks s " +
                "LEFT JOIN stack_articles sa ON s.id = sa.stack_id " +
                "GROUP BY s.id ORDER BY s.updated_at DESC")) {
            while (rs.next()) {
                Map<String, Object> stack = new HashMap<>();
                stack.put("id", rs.getInt("id"));
                stack.put("name", rs.getString("name"));
                stack.put("keywords", rs.getString("keywords"));
                stack.put("focus", rs.getString("focus"));
                stack.put("searchDepth", rs.getInt("search_depth"));
                stack.put("status", rs.getString("status"));
                stack.put("articleCount", rs.getInt("article_count"));
                stack.put("createdAt", rs.getString("created_at"));
                stack.put("updatedAt", rs.getString("updated_at"));
                stacks.add(stack);
            }
        } catch (SQLException e) {
            logger.error("Error getting all stacks", e);
        }
        return stacks;
    }

    public void updateStack(int id, String focus, int searchDepth) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE stacks SET focus = ?, search_depth = ?, updated_at = NOW() WHERE id = ?")) {
            ps.setString(1, focus);
            ps.setInt(2, Math.min(searchDepth, 30));
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating stack: {}", id, e);
        }
    }

    public void deleteStack(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stacks WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting stack: {}", id, e);
        }
    }

    public void updateStatus(int id, String status) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE stacks SET status = ?, updated_at = NOW() WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating stack status: {}", id, e);
        }
    }

    public void updateAnalysis(int id, String analysisJson) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE stacks SET analysis = ?, status = 'ready', updated_at = NOW() WHERE id = ?")) {
            ps.setString(1, analysisJson);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating stack analysis: {}", id, e);
        }
    }

    public void updateKeywords(int id, String keywords) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE stacks SET keywords = ?, updated_at = NOW() WHERE id = ?")) {
            ps.setString(1, keywords);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating stack keywords: {}", id, e);
        }
    }

    public void addArticleToStack(int stackId, int articleId, String source) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO stack_articles (stack_id, article_id, source) VALUES (?, ?, ?) " +
                "ON CONFLICT (stack_id, article_id) DO NOTHING")) {
            ps.setInt(1, stackId);
            ps.setInt(2, articleId);
            ps.setString(3, source != null ? source : "manual");
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding article {} to stack {}", articleId, stackId, e);
        }
    }

    public void removeArticleFromStack(int stackId, int articleId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM stack_articles WHERE stack_id = ? AND article_id = ?")) {
            ps.setInt(1, stackId);
            ps.setInt(2, articleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error removing article {} from stack {}", articleId, stackId, e);
        }
    }

    public List<Map<String, Object>> getStackArticles(int stackId) {
        List<Map<String, Object>> articles = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT a.id, a.title, a.keywords, a.images, a.created_at, sa.source, sa.added_at " +
                "FROM stack_articles sa JOIN articles a ON sa.article_id = a.id " +
                "WHERE sa.stack_id = ? ORDER BY sa.added_at DESC")) {
            ps.setInt(1, stackId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> article = new HashMap<>();
                    article.put("id", rs.getInt("id"));
                    article.put("title", rs.getString("title"));
                    article.put("keywords", rs.getString("keywords"));
                    article.put("images", rs.getString("images"));
                    article.put("source", rs.getString("source"));
                    article.put("addedAt", rs.getString("added_at"));
                    articles.add(article);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting articles for stack: {}", stackId, e);
        }
        return articles;
    }
}
