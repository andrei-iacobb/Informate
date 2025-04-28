/**
 * MOD003484
 * SID: 2402513
 * Project: Informate
 * Author: Andrei
 * 
 * Main application class for the Informate Backend CLI.
 * This class serves as the entry point for the application and manages the user interface,
 * coordinating between different components:
 * - User authentication (via auth class)
 * - Web scraping (via scraper class)
 * - Article processing (via AI class)
 * - Database operations (via articles class)
 * 
 * The application follows a command-line interface pattern where users can:
 * 1. Register/Login to access the system
 * 2. Add new articles by providing URLs
 * 3. View summaries of stored articles
 * 4. Access full article details
 */

package com.example.informate;

import java.io.*;
import java.util.*;

/**
 * Main application class for the Informate Backend CLI.
 * Handles user interaction, authentication, web scraping, AI processing,
 * and database operations for managing news articles.
 */
public class main {
    // Instantiate necessary service classes
    static scraper scrap = new scraper();
    static auth au = new auth();
    static articles art = new articles();
    static AI ai = new AI();

    /**
     * Application entry point. Initializes the system and presents the initial authentication menu.
     * This method:
     * 1. Starts the backend CLI interface
     * 2. Initializes the authentication database
     * 3. Verifies console availability for secure password input
     * 4. Presents the initial login/register menu
     * 5. Handles user choice and directs to appropriate authentication flow
     *
     * @param args Command line arguments (not used in current implementation)
     */
    public static void main(String[] args) {
        System.out.println("Starting Informate Backend CLI");
        
        // Initialize the authentication database
        au.initialiseDB();

        // Get console instance for secure password input
        Console console = System.console();
        if (console == null) {
            System.err.println("Error: Couldn't get Console instance. CLI password input requires a console.");
            System.exit(1);
        }

        try (Scanner input = new Scanner(System.in)) {
            System.out.println("Welcome to Informate Backend CLI");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.print("> ");
            String option = input.next();

            switch (option) {
                case "1":
                    handleLogin(input, console);
                    break;
                case "2":
                    handleRegistration(input, console);
                    break;
                default:
                    System.out.println("Invalid option selected. Exiting.");
                    break;
            }
        }
    }

    /**
     * Handles the user login process.
     * This method:
     * 1. Prompts for username
     * 2. Attempts authentication via auth class
     * 3. Validates the returned token
     * 4. On successful login, launches the main menu
     * 5. On failed login, displays appropriate error message
     *
     * @param input Scanner instance for reading user input
     * @param console Console instance for secure password reading
     */
    private static void handleLogin(Scanner input, Console console) {
        System.out.println("Enter username:");
        input.nextLine();
        String username = input.nextLine();

        String token = au.login(username);

        if (token != null && au.isTokenValid(token)) {
            System.out.println("Login successful.");
            runMainMenu(input);
        } else {
            System.out.println("Login failed. Please check username and password.");
        }
    }

    /**
     * Handles new user registration.
     * This method:
     * 1. Prompts for desired username
     * 2. Securely reads password using Console
     * 3. Registers user details in the authentication database
     * 4. Provides feedback on registration status
     *
     * Security note: Passwords are read securely and immediately cleared from memory
     *
     * @param input Scanner instance for reading user input
     * @param console Console instance for secure password reading
     */
    private static void handleRegistration(Scanner input, Console console) {
        System.out.println("Choose a username:");
        input.nextLine();
        String username = input.nextLine();

        System.out.println("Choose a password:");
        char[] passwordChars = console.readPassword("Password: ");
        String password = new String(passwordChars);
        Arrays.fill(passwordChars, ' ');

        au.insertDetails(username, password);
        System.out.println("Registration complete. You can now login.");
    }

    /**
     * Manages the main application menu and user interactions after successful login.
     * Presents a loop of options for:
     * 1. Adding new articles (scraping + AI processing)
     * 2. Viewing all stored article summaries
     * 3. Accessing full article details by title
     * 4. Exiting the application
     *
     * This method handles the core application flow and coordinates between
     * different components based on user choices.
     *
     * @param input Scanner instance for reading user input
     */
    private static void runMainMenu(Scanner input) {
        boolean running = true;
        while (running) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Add new article (scrape + summarize)");
            System.out.println("2. View all article summaries");
            System.out.println("3. View full article by title");
            System.out.println("4. Exit");
            System.out.print("> ");
            String choice = input.next();

            switch (choice) {
                case "1":
                    handleNewArticle(input);
                    break;
                case "2":
                    viewAllSummaries();
                    break;
                case "3":
                    viewFullArticle(input);
                    break;
                case "4":
                    System.out.println("Exiting application. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid input. Please enter a number between 1 and 4.");
            }
        }
        au.closeConnection();
        System.exit(0);
    }

    /**
     * Processes the addition of a new article.
     * This method orchestrates the complete article processing pipeline:
     * 1. Accepts and validates the article URL
     * 2. Scrapes the article title
     * 3. Extracts article text content
     * 4. Downloads relevant images
     * 5. Stores raw article text
     * 6. Processes content with AI for summary and keywords
     * 7. Updates the database with complete article information
     *
     * Error handling:
     * - Validates URL and content at each step
     * - Provides detailed feedback on progress and errors
     * - Ensures partial data is saved even if later steps fail
     *
     * @param input Scanner instance for reading the article URL
     */
    private static void handleNewArticle(Scanner input) {
        try {
            System.out.println("Paste the website URL:");
            input.nextLine();
            String url = input.nextLine().trim();

            if (url.isEmpty()) {
                System.err.println("Error: URL cannot be empty");
                return;
            }

            System.out.println("\nScraping website...");
            
            // Get the article title
            String title = scrap.scrapeForTitle(url);
            if (title == null || title.trim().isEmpty()) {
                System.err.println("Error: Could not find article title");
                return;
            }
            System.out.println("Title: " + title);

            // Get the article text
            String articleText = scrap.scrapePageForText(url);
            if (articleText == null || articleText.trim().isEmpty()) {
                System.err.println("Error: Could not extract article text");
                return;
            }
            System.out.println("Extracted " + articleText.split("\\s+").length + " words");

            // Get images if available
            List<String> images = scrap.scrapePageForImages(url, title);
            if (!images.isEmpty()) {
                System.out.println("Found " + images.size() + " relevant images");
            }

            // Save the raw text
            System.out.println("\nSaving article to database...");
            art.insertRawText(title, articleText);

            // Process with AI
            System.out.println("Processing with AI...");
            AI.processArticle(articleText, title, images);

            System.out.println("\nArticle successfully added and processed!");

        } catch (Exception e) {
            System.err.println("\nError processing article: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and displays summaries of all stored articles.
     * This method:
     * 1. Queries the database for all stored articles
     * 2. Formats and displays each article's:
     *    - Title
     *    - Summary
     * 3. Handles empty database case
     * 4. Provides user-friendly output formatting
     */
    private static void viewAllSummaries() {
        System.out.println("\nRetrieving article summaries...");
        List<Map<String, String>> allArticles = art.getAllArticles();

        if (allArticles.isEmpty()) {
            System.out.println("No articles have been saved yet.");
            return;
        }

        System.out.println("\n--- Saved Article Summaries ---");
        for (Map<String, String> article : allArticles) {
            String title = article.getOrDefault("title", "[No Title]");
            String summary = article.getOrDefault("summary", "[Summary not available]");
            System.out.println("\n- " + title + ":");
            System.out.println("  " + summary);
        }
    }

    /**
     * Retrieves and displays complete information for a specific article.
     * This method:
     * 1. Prompts for article title
     * 2. Queries the database for exact title match
     * 3. Displays comprehensive article information:
     *    - Title
     *    - Summary
     *    - Keywords
     *    - Associated images
     *    - Full raw text
     * 4. Handles case where article is not found
     *
     * @param input Scanner instance for reading the article title
     */
    private static void viewFullArticle(Scanner input) {
        System.out.println("\nEnter the exact title of the article to view:");
        input.nextLine();
        String title = input.nextLine();

        System.out.println("Retrieving article details for: " + title);
        Map<String, String> article = art.getArticleByTitle(title);

        if (article == null || article.isEmpty()) {
            System.out.println("Article with title '" + title + "' not found.");
            return;
        }

        System.out.println("\n--- Full Article Details ---");
        System.out.println("Title: " + article.getOrDefault("title", "N/A"));
        System.out.println("\nSummary: " + article.getOrDefault("summary", "[Not processed yet]"));
        System.out.println("\nKeywords: " + article.getOrDefault("keywords", "[Not processed yet]"));
        System.out.println("\nImages: " + article.getOrDefault("images", "[None]"));
        System.out.println("\nRaw Text: " + article.getOrDefault("rawText", "[Not available]"));
        System.out.println("\n------");
    }
}
