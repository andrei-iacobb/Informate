/**
 * MOD003484
 * SID: 2402513
 * Project: Informate
 * Author: Andrei
 */

package com.example.informate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the storage and retrieval of article data in a SQLite database (`articles.db`).
 * Handles operations such as inserting raw article text, updating articles with summaries and keywords,
 * and retrieving article data by title or all articles.
 */
public class articles {
    private static final Logger logger = LoggerFactory.getLogger(articles.class);

    // Database connection - consider making this non-static if multiple instances are needed
    private Connection conn;

    /**
     * Constructor for the articles class.
     * Initializes the connection to the SQLite database (`articles.db`).
     * Creates the `articles` table if it doesn't already exist.
     * Handles potential SQLExceptions during setup.
     */
    public articles() {
        try {
            // Establish connection to the SQLite database file
            conn = DriverManager.getConnection("jdbc:sqlite:articles.db");
            // Use try-with-resources for the Statement to ensure it's closed
            try (Statement stmt = conn.createStatement()) {
                // SQL command to create the articles table if it doesn't exist
                // Stores title (primary key), summary, keywords, raw text, and image filenames
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS articles (
                        title TEXT PRIMARY KEY,
                        summary TEXT,
                        keywords TEXT,
                        rawText TEXT,
                        images TEXT
                    )
                """);
            }
            logger.info("Articles database initialized successfully");
        } catch (SQLException e) {
            logger.error("FATAL: Failed to initialize articles database", e);
            System.exit(1);
        }
    }

    /**
     * Inserts raw article text into the database.
     * If an article with the same title already exists, the operation is ignored (INSERT OR IGNORE).
     *
     * @param title The title of the article.
     * @param rawText The raw text content of the article.
     */
    public void insertRawText(String title, String rawText) {
        logger.debug("Inserting raw text for article: {}", title);
        
        // Use try-with-resources for the PreparedStatement
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO articles (title, rawText) VALUES (?, ?)")) {
            ps.setString(1, title); // Set the title parameter
            ps.setString(2, rawText); // Set the raw text parameter
            ps.executeUpdate(); // Execute the insert operation
            logger.info("Raw text inserted for article: {}", title);
        } catch (SQLException e) {
            logger.error("Error inserting raw text for article: {}", title, e);
        }
    }

    /**
     * Updates an existing article with its summary, keywords, and associated image filenames.
     * The article must already exist in the database (inserted via insertRawText).
     *
     * @param title The title of the article to update.
     * @param summary The summarized text of the article.
     * @param keywords A list of keywords extracted from the article.
     * @param images A list of filenames for images associated with the article.
     */
    public void insertCompleteArticle(String title, String summary, List<String> keywords, List<String> images) {
        logger.debug("Updating article with summary and keywords: {}", title);
        
        // Convert lists to comma-separated strings for database storage
        String keywordsString = String.join(",", keywords);
        String imagesString = String.join(",", images);

        // Use try-with-resources for the PreparedStatement
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE articles SET summary = ?, keywords = ?, images = ? WHERE title = ?")) {
            ps.setString(1, summary); // Set the summary parameter
            ps.setString(2, keywordsString); // Set the keywords parameter
            ps.setString(3, imagesString); // Set the images parameter
            ps.setString(4, title); // Set the title parameter for the WHERE clause
            ps.executeUpdate(); // Execute the update operation
            logger.info("Article updated with summary, keywords, and images: {}", title);
        } catch (SQLException e) {
            logger.error("Error updating article: {}", title, e);
        }
    }

    /**
     * Retrieves all articles stored in the database.
     * Each article is returned as a Map containing its title, summary, keywords, raw text, and images.
     *
     * @return A list of Maps, each representing an article's data.
     */
    public List<Map<String, String>> getAllArticles() {
        logger.debug("Retrieving all articles from database");
        
        List<Map<String, String>> articlesList = new ArrayList<>();
        // Use try-with-resources for the Statement and ResultSet
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM articles")) {
            // Iterate through each row in the result set
            while (rs.next()) {
                // Create a Map to hold the current article's data
                Map<String, String> article = new HashMap<>();
                // Populate the Map with data from the current row
                article.put("title", rs.getString("title"));
                article.put("summary", rs.getString("summary"));
                article.put("keywords", rs.getString("keywords"));
                article.put("rawText", rs.getString("rawText"));
                article.put("images", rs.getString("images"));
                // Add the article Map to the list
                articlesList.add(article);
            }
            logger.info("Retrieved {} articles from the database", articlesList.size());
        } catch (SQLException e) {
            logger.error("Error retrieving all articles", e);
        }
        return articlesList;
    }

    /**
     * Retrieves a specific article by its title.
     * Returns an empty Map if the article is not found.
     *
     * @param title The title of the article to retrieve.
     * @return A Map containing the article's data, or an empty Map if not found.
     */
    public Map<String, String> getArticleByTitle(String title) {
        logger.debug("Retrieving article by title: {}", title);
        
        Map<String, String> article = new HashMap<>();
        // Use try-with-resources for the PreparedStatement and ResultSet
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM articles WHERE title = ?")) {
            ps.setString(1, title); // Set the title parameter
            try (ResultSet rs = ps.executeQuery()) {
                // If a row is found, populate the Map with the article's data
                if (rs.next()) {
                    article.put("title", rs.getString("title"));
                    article.put("summary", rs.getString("summary"));
                    article.put("keywords", rs.getString("keywords"));
                    article.put("rawText", rs.getString("rawText"));
                    article.put("images", rs.getString("images"));
                    logger.info("Retrieved article: {}", title);
                } else {
                    logger.warn("Article not found: {}", title);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving article: {}", title, e);
        }
        return article;
    }
}
