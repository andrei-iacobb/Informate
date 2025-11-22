/**
 * MOD003484
 * SID: 2402513
 * Project: Informate
 * Author: Andrei
 */

package com.example.informate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles web scraping tasks using the Jsoup library.
 * Responsible for fetching article titles, main text content, and relevant images from a given URL.
 * Saves downloaded images to a designated directory (`SiteImages/`).
 */
public class scraper {
    private static final Logger logger = LoggerFactory.getLogger(scraper.class);

    // Directory to save downloaded images
    private static final String IMAGE_DIR = "/app/SiteImages/";

    /**
     * Constructor for the scraper.
     * Creates the image directory (`SiteImages/`) if it doesn't exist.
     * Handles potential IOExceptions during setup.
     */
    public scraper() {
        try {
            // Ensure the directory for storing images exists
            Files.createDirectories(Paths.get(IMAGE_DIR));
            logger.info("Scraper initialized. Image directory: {}", IMAGE_DIR);
        } catch (IOException e) {
            logger.error("FATAL: Error initializing scraper (file I/O)", e);
        }
    }

    /**
     * Scrapes the given URL for the main article title (typically the first H1 tag).
     *
     * @param pageURL The URL of the web page to scrape.
     * @return The scraped article title, or a default title ("Untitled Article") if no H1 tag is found.
     * @throws IOException If Jsoup fails to connect to or parse the URL.
     * @throws IllegalArgumentException if pageURL is null or empty
     */
    public String scrapeForTitle(String pageURL) throws IOException {
        logger.debug("Scraping title from URL: {}", pageURL);
        
        if (pageURL == null || pageURL.trim().isEmpty()) {
            logger.error("Page URL is null or empty");
            throw new IllegalArgumentException("Page URL cannot be null or empty");
        }

        // Connect to the URL and parse the HTML document
        Document doc = Jsoup.connect(pageURL).get();

        // Select the first H1 element, commonly used for main titles
        Element titleElement = doc.selectFirst("h1");

        // Determine the title text, use default if H1 is not found
        String title = (titleElement != null) ? titleElement.text() : "Untitled Article";

        logger.info("Scraped title: {} from {}", title, pageURL);
        return title;
    }

    /**
     * Scrapes the main textual content (paragraphs within the `<main>` tag) from the given URL.
     *
     * @param pageURL The URL of the web page to scrape.
     * @return The combined text of all found paragraphs.
     * @throws IOException If Jsoup fails to connect to or parse the URL.
     * @throws IllegalArgumentException if pageURL is null or empty
     */
    public String scrapePageForText(String pageURL) throws IOException {
        logger.debug("Scraping text content from URL: {}", pageURL);
        
        if (pageURL == null || pageURL.trim().isEmpty()) {
            logger.error("Page URL is null or empty");
            throw new IllegalArgumentException("Page URL cannot be null or empty");
        }

        // Connect and parse with additional headers
        // To allow connection to the website, need to add the user agent and accept headers
        Document doc = Jsoup.connect(pageURL)
            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .followRedirects(true)
            .timeout(30000)
            .get();

        // First try to find the main article content
        Elements contentElements = doc.select("article, [role='main'], main, .article__body-content, .story-body__inner");
        StringBuilder content = new StringBuilder();

        if (!contentElements.isEmpty()) {
            // Found main content container, extract paragraphs
            for (Element element : contentElements) {
                Elements paragraphs = element.select("p");
                for (Element p : paragraphs) {
                    String text = p.text().trim();
                    if (!text.isEmpty() && text.length() > 20) {
                        content.append(text).append(" ");
                    }
                }
            }
        }

        // If no content found, try direct paragraph selection
        if (content.length() == 0) {
            Elements paragraphs = doc.select("p");
            for (Element p : paragraphs) {
                // Skip navigation, headers, footers
                if (p.parents().stream().anyMatch(e -> 
                    e.tagName().matches("nav|header|footer") ||
                    e.className().toLowerCase().matches(".*nav.*|.*header.*|.*footer.*"))) {
                    continue;
                }
                String text = p.text().trim();
                if (!text.isEmpty() && text.length() > 20) {
                    content.append(text).append(" ");
                }
            }
        }

        String result = content.toString().trim();
        
        if (result.isEmpty()) {
            logger.error("No content found at URL: {}", pageURL);
            return "";
        }

        logger.info("Successfully extracted {} characters of text", result.length());
        return result;
    }

    /**
     * Scrapes images (`<img>` tags) from the page.
     * Attempts to download images that seem relevant based on a simple keyword match
     * between the image's alt/title attributes and words from the article title.
     * Saves relevant images to the IMAGE_DIR with random filenames.
     *
     * @param pageURL The URL of the web page to scrape.
     * @param articleTitle The title of the article (used for keyword matching).
     * @return A list of filenames (relative to IMAGE_DIR) of the successfully downloaded relevant images.
     * @throws IOException If Jsoup fails to connect to or parse the URL.
     * @throws IllegalArgumentException if pageURL or articleTitle is null or empty
     */
    public List<String> scrapePageForImages(String pageURL, String articleTitle) throws IOException {
        logger.debug("Scraping images from URL: {} for article: {}", pageURL, articleTitle);

        if (pageURL == null || pageURL.trim().isEmpty()) {
            logger.error("Page URL is null or empty");
            throw new IllegalArgumentException("Page URL cannot be null or empty");
        }
        if (articleTitle == null || articleTitle.trim().isEmpty()) {
            logger.error("Article title is null or empty");
            throw new IllegalArgumentException("Article title cannot be null or empty");
        }

        // Connect and parse
        Document doc = Jsoup.connect(pageURL)
            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .followRedirects(true)
            .timeout(30000)
            .get();

        List<String> savedImages = new ArrayList<>();

        // First, try to find images within article content
        Elements articleImages = doc.select("article img, [role='main'] img, main img, .article__body-content img, .story-body__inner img");

        logger.debug("Found {} images in article content area", articleImages.size());

        // If we found images in the article, use those (more likely to be relevant)
        Elements targetImages = !articleImages.isEmpty() ? articleImages : doc.select("img");

        logger.debug("Processing {} images for download", targetImages.size());

        // Prepare keywords from the article title for relevance check
        String[] titleWords = Arrays.stream(articleTitle.toLowerCase().split("\\s+"))
                .filter(word -> word.length() > 3)
                .toArray(String[]::new);

        int savedCount = 0;
        int maxImages = 8; // Limit to 8 images per article

        // Iterate through each found image element
        for (Element image : targetImages) {
            if (savedCount >= maxImages) break; // Stop if we have enough images

            // Get the absolute source URL of the image
            String src = image.absUrl("src");
            if (src == null || src.isEmpty()) continue;

            // Skip small images (likely icons, logos, etc.)
            try {
                String width = image.attr("width");
                String height = image.attr("height");
                if (!width.isEmpty() && !height.isEmpty()) {
                    int w = Integer.parseInt(width);
                    int h = Integer.parseInt(height);
                    if (w < 200 || h < 150) {
                        logger.debug("Skipping small image: {}x{}", w, h);
                        continue; // Skip thumbnails and icons
                    }
                }
            } catch (NumberFormatException e) {
                // If width/height parsing fails, continue anyway
            }

            // Skip common non-article images
            String srcLower = src.toLowerCase();
            if (srcLower.contains("logo") || srcLower.contains("icon") ||
                srcLower.contains("avatar") || srcLower.contains("badge")) {
                continue;
            }

            // Get image alt and title attributes
            String altText = image.attr("alt").toLowerCase();
            String titleText = image.attr("title").toLowerCase();
            String combinedText = altText + " " + titleText;

            // For images in article content, be more lenient
            boolean relevant = !articleImages.isEmpty() ||
                              Arrays.stream(titleWords).anyMatch(combinedText::contains) ||
                              !altText.isEmpty(); // Has alt text (likely meaningful)

            if (relevant) {
                logger.debug("Downloading image: {}", src);
                try (InputStream input = new URL(src).openStream()) {
                    String extension = getFileExtension(src);
                    String fileName = generateRandomFileName() + extension;
                    java.nio.file.Path targetPath = Paths.get(IMAGE_DIR + fileName);
                    Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    savedImages.add(fileName);
                    savedCount++;
                    logger.debug("Saved image {} as: {}", savedCount, fileName);
                } catch (IOException e) {
                    logger.error("Failed to download image: {}", src, e);
                }
            }
        }

        logger.info("Downloaded {} images to {}", savedImages.size(), IMAGE_DIR);
        return savedImages;
    }

    /**
     * Generates a random alphanumeric filename string (8 characters long).
     *
     * @return A random 8-character string.
     */
    private String generateRandomFileName() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random rand = new Random();
        StringBuilder filename = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            filename.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return filename.toString();
    }

    /**
     * Extracts the file extension from a URL string.
     *
     * @param urlString The URL of the file.
     * @return The file extension (including the dot, e.g., ".jpg"), or ".jpg" as a default if none found.
     */
    private String getFileExtension(String urlString) {
        try {
            String path = new URL(urlString).getPath();
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0 && lastDot > path.lastIndexOf('/')) {
                String ext = path.substring(lastDot).toLowerCase();
                // Basic check for common image extensions
                if (ext.matches("\\.(jpe?g|png|gif|webp|bmp)")) {
                    return ext;
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting file extension from URL: {}", urlString, e);
        }
        // Return a default extension if extraction fails or it's not a common image type
        return ".jpg";
    }
}
