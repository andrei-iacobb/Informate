/**
 * MOD003484
 * SID: 2402513
 * Project: Informate
 * Author: Andrei
 * 
 * Utility class for loading environment variables from .env file
 */

package com.example.informate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading environment variables from a .env file.
 * This class provides a simple way to load configuration values from a .env file
 * which is commonly used for storing sensitive information like API keys.
 */
public class EnvLoader {
    private static Map<String, String> envVars = new HashMap<>();
    private static boolean loaded = false;
    
    /**
     * Loads environment variables from the .env file.
     * The .env file should be in the project root directory.
     */
    public static void loadEnv() {
        if (loaded) {
            return; // Already loaded
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse KEY=VALUE format
                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String key = line.substring(0, equalIndex).trim();
                    String value = line.substring(equalIndex + 1).trim();
                    
                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    envVars.put(key, value);
                }
            }
            loaded = true;
            System.out.println("Environment variables loaded from .env file");
        } catch (IOException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
            System.err.println("Please create a .env file in the project root with your OpenAI API key");
        }
    }
    
    /**
     * Gets an environment variable value.
     * First checks the .env file, then falls back to system environment variables.
     * 
     * @param key The environment variable key
     * @return The environment variable value, or null if not found
     */
    public static String getEnv(String key) {
        loadEnv(); // Ensure .env file is loaded
        
        // First check .env file
        String value = envVars.get(key);
        if (value != null) {
            return value;
        }
        
        // Fall back to system environment variables
        return System.getenv(key);
    }
    
    /**
     * Gets an environment variable value with a default fallback.
     * 
     * @param key The environment variable key
     * @param defaultValue The default value to return if key is not found
     * @return The environment variable value, or the default value if not found
     */
    public static String getEnv(String key, String defaultValue) {
        String value = getEnv(key);
        return value != null ? value : defaultValue;
    }
} 