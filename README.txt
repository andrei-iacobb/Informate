INFORMATE - News Article Scraper and Summarizer
=========================================

This program allows you to scrape news articles from websites, automatically summarize them using AI, and store them locally for later reference.

System Requirements
-----------------
- Windows 10 or later
- Java Development Kit (JDK) 17 or later
- Maven (for dependency management)

Setup Instructions
-----------------
1. Install Java Development Kit (JDK):
   - Download OpenJDK 17 from: https://adoptium.net/
   - Run the installer and follow the prompts
   - Verify installation by opening Command Prompt and typing: java -version

2. Install Maven:
   - Download Maven from: https://maven.apache.org/download.cgi
   - Extract the ZIP file to a location (e.g., C:\Program Files\Maven)
   - Add Maven's bin directory to your PATH environment variable
   - Verify installation by opening Command Prompt and typing: mvn -version

3. Set up the project:
   - Extract the project files to a directory (e.g., C:\Projects\Informate)
   - Open Command Prompt and navigate to the project directory:
     cd C:\Projects\Informate\Backend\informate

4. Build the project:
   - Run the following command:
     mvn clean install

5. Get an OpenAI API Key:
   - Visit https://platform.openai.com/api-keys
   - Create an account or log in
   - Generate a new API key
   - Replace the API_KEY value in AI.java with your key

Running the Program
------------------
1. In the Command Prompt, make sure you're in the project directory:
   cd C:\Projects\Informate\Backend\informate

2. Run the program:
   mvn exec:java -Dexec.mainClass="com.example.informate.main"

Using the Program
----------------
1. When the program starts, you'll be prompted to:
   - Register (first time) or Login (subsequent uses)
   - Follow the prompts to create an account

2. Main Menu Options:
   1. Add new article
      - Paste the URL of a news article when prompted
      - The program will automatically:
        * Extract the article content
        * Generate a summary
        * Extract keywords
        * Download relevant images
   
   2. View all article summaries
      - Shows a list of all saved articles with their summaries
   
   3. View full article by title
      - Enter the exact title of an article
      - Shows complete details including:
        * Full text
        * Summary
        * Keywords
        * Associated images
   
   4. Exit
      - Closes the program

Notes
-----
- The program automatically creates necessary databases when first run
- Images are saved in a 'SiteImages' directory
- The program works best with news websites
- If an article fails to load, try copying the URL directly from your browser
- Make sure your OpenAI API key is valid and has sufficient credits

Troubleshooting
--------------
1. If you get "command not found" errors:
   - Verify Java and Maven are properly installed
   - Check your PATH environment variables

2. If article scraping fails:
   - Verify you can access the article in your browser
   - Check if the URL is complete and correct
   - Some websites may block automated access

3. If summarization fails:
   - Verify your OpenAI API key is correctly set
   - Check your internet connection
   - Ensure the article text was successfully scraped

For additional help or to report issues, please contact Andrei Iacob @ AGI105@student.aru.ac.uk