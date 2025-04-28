/**
 * MOD003484
 * SID: 2402513
 * Project: Informate
 * Author: Andrei
 */

package com.example.informate;
import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles user authentication, registration, and secure password management.
 * Stores user credentials in a SQLite database (`data.db`).
 * Uses SHA-512 hashing with salt for password storage.
 * Manages temporary session tokens in memory.
 */
public class auth {
    // Database connection - consider making this non-static if auth instances are needed
    private Connection conn;

    // In-memory store for session tokens (Token -> Username)
    // Note: This is temporary and tokens will be lost on application restart.
    // For persistent sessions, store tokens in the database or use a dedicated session management system.
    private Map<String, String> tokenStore = new HashMap<>();

    /**
     * Initializes the connection to the SQLite database (`data.db`).
     * Creates the `user` table if it doesn't already exist.
     * Should be called once at application startup.
     */
    public void initialiseDB(){
        try{
            // Establish connection to the SQLite database file
            conn = DriverManager.getConnection("jdbc:sqlite:data.db");
            // Use try-with-resources for the Statement to ensure it's closed
            try (Statement stmt = conn.createStatement()) {
                // SQL command to create the user table if it doesn't exist
                // Stores username (primary key) and hashed password
                stmt.execute("CREATE TABLE IF NOT EXISTS user (username TEXT PRIMARY KEY, password TEXT)");
            }
            System.out.println("Authentication database initialized successfully.");
        } catch (SQLException e){
            // Log or handle the exception appropriately
            System.err.println("FATAL: Failed to initialize authentication database: " + e.getMessage());
            e.printStackTrace();
            // Consider exiting the application if the DB connection fails
             System.exit(1);
        }
    }

    /**
     * Inserts a new user into the database.
     * Hashes the provided password with a unique salt before storing.
     *
     * @param username The username for the new user.
     * @param password The plain-text password for the new user.
     */
    public void insertDetails(String username, String password){
        // Use try-with-resources for the PreparedStatement
        // The SQL statement inserts username and the hashed password
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO user (username, password) VALUES (?, ?) ")){
            ps.setString(1, username); // Set the username parameter
            ps.setString(2, hashPassword(password)); // Hash the password and set the parameter
            ps.executeUpdate(); // Execute the insert operation
            System.out.println("User '" + username + "' registered successfully.");
        } catch (SQLException e) {
            // Handle potential SQL errors (e.g., username already exists)
            if (e.getErrorCode() == 19) { // SQLite constraint violation (likely PRIMARY KEY)
                 System.err.println("Error: Username '" + username + "' already exists.");
            } else {
                 System.err.println("Error inserting user details: " + e.getMessage());
                 e.printStackTrace();
            }
        } catch (NoSuchAlgorithmException e) {
            // Handle errors during password hashing
            System.err.println("Error hashing password: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
             // Catch any other unexpected exceptions
             System.err.println("An unexpected error occurred during registration: " + e.getMessage());
             e.printStackTrace();
        }
    }

    /**
     * Attempts to log in a user.
     * 1. Checks if the username exists.
     * 2. Prompts for the password using the Console.
     * 3. Verifies the entered password against the stored hash.
     * 4. If verification succeeds, generates and stores a session token.
     *
     * @param username The username attempting to log in.
     * @return A session token string if login is successful, null otherwise.
     */
    public String login(String username){
        // Check if the username exists in the database
        try (PreparedStatement ps = conn.prepareStatement("SELECT username FROM user WHERE username = ?")){
            ps.setString(1, username); // Set the username parameter
            ResultSet result = ps.executeQuery();

            // If a result is found, the username exists
            if (result.next()){
                System.out.println("Username found. Please enter password.");

                // Get console for secure password input
                Console console = System.console();
                if (console == null) {
                    System.err.println("Error: Cannot get Console instance for password input.");
                    // Cannot proceed without console for password
                    return null;
                }

                // Read password securely without echoing
                char[] passwordChar = console.readPassword("Password: ");
                String password = new String(passwordChar);
                Arrays.fill(passwordChar, ' '); // Clear password from memory

                try {
                    // Verify the entered password against the stored hash
                    if(verifyPassword(password, username)){
                        System.out.println("Password correct. Login successful.");
                        // Generate a secure session token upon successful verification
                        return generateSecureToken(username);
                    }
                    else{
                        System.err.println("Incorrect password.");
                        return null; // Return null if password verification fails
                    }
                } catch (Exception e) {
                    // Handle errors during password verification (e.g., hashing algorithm not found)
                    System.err.println("Error during password verification: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            } else {
                // Username not found in the database
                System.err.println("Username '" + username + "' not found.");
                return null;
            }
        } catch (SQLException e) {
            // Handle potential database errors during the user lookup
            System.err.println("Database error during login: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Verifies a given plain-text password against the stored hash for a specific user.
     * Retrieves the stored hash and salt, re-hashes the input password with the same salt,
     * and compares the results.
     *
     * @param inputPassword The plain-text password entered by the user.
     * @param username The username whose password needs verification.
     * @return true if the password matches the stored hash, false otherwise.
     * @throws NoSuchAlgorithmException If the SHA-512 algorithm is not available.
     * @throws SQLException If there is an error retrieving data from the database.
     */
    public boolean verifyPassword(String inputPassword, String username) throws NoSuchAlgorithmException, SQLException {
        String storedPasswordData = null;

        // Retrieve the stored password data (hash:salt) for the given username
        try(PreparedStatement ps = conn.prepareStatement("SELECT password FROM user WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet result = ps.executeQuery();
            if(result.next()){ // Check if user was found
                storedPasswordData = result.getString("password");
            } else {
                // Should not happen if called after username check in login, but good practice
                System.err.println("Verification error: User '" + username + "' not found during password retrieval.");
                return false;
            }
        } // PreparedStatement and ResultSet are auto-closed here

        // If no password data was found (shouldn't happen based on above check)
        if (storedPasswordData == null) {
             System.err.println("Verification error: No stored password data found for user '" + username + ".");
             return false;
        }

        // Split the stored data into hash and salt
        String[] parts = storedPasswordData.split(":");
        if (parts.length != 2) {
            // Data format error in the database
            System.err.println("Verification error: Invalid stored password format for user '" + username + ".");
            // Potentially log this as a critical data integrity issue
            return false;
        }
        String storedHash = parts[0];
        byte[] salt = Base64.getDecoder().decode(parts[1]); // Decode the Base64 encoded salt

        // Hash the input password using the retrieved salt
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt); // Apply the same salt
        byte[] hashedInputPasswordBytes = md.digest(inputPassword.getBytes(StandardCharsets.UTF_8)); // Use UTF-8 encoding
        String hashedInputPassword = Base64.getEncoder().encodeToString(hashedInputPasswordBytes);

        // Compare the newly generated hash with the stored hash
        // Use constant-time comparison if security is paramount, though MessageDigest.isEqual is often sufficient
        return MessageDigest.isEqual(hashedInputPassword.getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hashes a plain-text password using SHA-512 with a randomly generated salt.
     * The salt is appended to the hash, separated by a colon, and both are Base64 encoded.
     *
     * @param password The plain-text password to hash.
     * @return A string containing the Base64(hash):Base64(salt).
     * @throws NoSuchAlgorithmException If the SHA-512 algorithm is not available.
     */
    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        // Generate a secure random salt (16 bytes is common)
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        // Get SHA-512 message digest instance
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt); // Apply the salt first

        // Hash the password (ensure consistent byte encoding, e.g., UTF-8)
        byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));

        // Encode hash and salt to Base64 for safe string storage
        String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        // Return the combined string
        return hashBase64 + ":" + saltBase64;
    }

    /**
     * Generates a cryptographically secure random token, hashes it, and stores it
     * in the in-memory token store, associated with the given username.
     *
     * @param username The username to associate with the generated token.
     * @return The Base64 encoded hashed token string, or null if an error occurs.
     */
    public String generateSecureToken(String username) {
        try {
            // Generate secure random bytes for the token
            SecureRandom random = new SecureRandom();
            byte[] tokenBytes = new byte[32]; // 32 bytes = 256 bits
            random.nextBytes(tokenBytes);

            // Hash the token bytes for storage (optional, but adds layer if token leaks)
            // Using SHA-256 or SHA-512 for the token itself
            MessageDigest md = MessageDigest.getInstance("SHA-256"); // SHA-256 is sufficient for tokens
            byte[] hashedToken = md.digest(tokenBytes);

            // Encode the hashed token to a Base64 string for easy use/storage
            String tokenString = Base64.getUrlEncoder().withoutPadding().encodeToString(hashedToken); // Use URL-safe encoding

            // Store the token (key) and associated username (value) in the map
            tokenStore.put(tokenString, username);
            System.out.println("Generated and stored token for user: " + username);
            return tokenString;
        } catch (NoSuchAlgorithmException e) {
            // Handle error if hashing algorithm isn't found
            System.err.println("Error generating secure token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a given token exists in the in-memory token store.
     *
     * @param token The token to validate.
     * @return true if the token is found in the store, false otherwise.
     */
    public boolean isTokenValid(String token) {
        // Simple check if the token exists as a key in the map
        return token != null && tokenStore.containsKey(token);
    }

    /**
     * Retrieves the username associated with a given valid token.
     *
     * @param token The token whose associated username is needed.
     * @return The username string, or null if the token is not valid or not found.
     */
    public String getUsernameFromToken(String token) {
        // Retrieve the username from the map using the token as the key
        return tokenStore.get(token);
    }

    /**
     * Removes a token from the in-memory store, effectively logging the user out
     * for session-based authentication using this token.
     *
     * @param token The token to invalidate/remove.
     */
    public void logout(String token) {
        if (token != null) {
             String removedUser = tokenStore.remove(token);
             if (removedUser != null) {
                 System.out.println("Token invalidated for user: " + removedUser);
             } else {
                  System.out.println("Attempted to logout with an invalid or already removed token.");
             }
        } else {
             System.out.println("Attempted to logout with a null token.");
        }
    }

    /**
     * Closes the database connection.
     * Should be called when the application is shutting down to release resources.
     */
    public void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Authentication database connection closed.");
            }
        } catch (SQLException e) {
            // Log error during connection closing
            System.err.println("Error closing authentication database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
