# Informate - News Article Scraper and Summarizer

A full-stack application that scrapes news articles from websites, automatically summarizes them using AI, and stores them locally for reference.

## Features

- 🔍 **Web Scraping**: Extract article content from news websites
- 🤖 **AI Summarization**: Generate concise summaries using OpenAI GPT
- 🔑 **Keyword Extraction**: Automatically identify key topics
- 🖼️ **Image Download**: Save relevant images from articles
- 👤 **User Authentication**: Secure login and registration system
- 💾 **Local Storage**: SQLite database for article persistence
- 🌐 **REST API**: Backend API for frontend integration
- ⚛️ **React Frontend**: Modern, responsive web interface with dark mode
- 🎨 **Modern UI**: Clean, minimalist design with Tailwind CSS
- 📱 **Responsive Design**: Optimized for desktop, tablet, and mobile
- 🌙 **Dark Mode**: System-aware theme with manual toggle
- 🐳 **Docker Ready**: One-command deployment with Docker Compose

## Tech Stack

### Backend
- **Java 17+** with Maven
- **Spark Framework** for REST API
- **SQLite** for data storage
- **JSoup** for web scraping
- **OpenAI API** for AI summarization

### Frontend
- **React 19** with modern hooks
- **React Router** for navigation
- **Axios** for API communication
- **Context API** for state management
- **Tailwind CSS** for modern styling
- **Dark mode** support

### Deployment
- **Docker** & **Docker Compose** for containerization
- **Nginx** for serving frontend in production
- **Multi-stage builds** for optimized images

## Getting Started

You can run Informate either locally or using Docker. Docker is recommended for easier setup and deployment.

### Option 1: Docker (Recommended)

#### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- OpenAI API key

#### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/informate.git
   cd informate
   ```

2. **Create the proxy network**
   ```bash
   docker network create proxy
   ```

3. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env and add your OpenAI API key
   ```

4. **Run with Docker Compose**
   ```bash
   docker-compose up -d
   ```

5. **Access the application**
   - Frontend: http://localhost:3001
   - Backend API: http://localhost:8080

For detailed Docker documentation, see [DOCKER.md](DOCKER.md)

### Option 2: Local Installation

#### Prerequisites

- Java Development Kit (JDK) 17 or later
- Maven 3.6+
- Node.js 16+ and npm
- OpenAI API key

#### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/informate.git
   cd informate
   ```

2. **Set up environment variables**
   ```bash
   # Copy template files
   cp env.template .env
   cp Backend/informate/env.template Backend/informate/.env
   
   # Edit the .env files and add your OpenAI API key
   OPENAI_API_KEY=your_openai_api_key_here
   ```

3. **Build and run the backend**
   ```bash
   cd Backend/informate
   mvn clean install
   mvn exec:java -Dexec.mainClass="com.example.informate.main"
   ```

4. **Install and run the frontend**
   ```bash
   cd Frontend/informate-frontend
   npm install
   npm start
   ```

### Getting an OpenAI API Key

1. Visit [OpenAI API Keys](https://platform.openai.com/api-keys)
2. Create an account or log in
3. Generate a new API key
4. Copy the key (starts with `sk-...`)
5. Add it to your `.env` files

## Usage

### Web Interface

1. Open your browser to `http://localhost:3000`
2. Register a new account or log in
3. Use the dashboard to:
   - Add new articles by URL
   - View article summaries
   - Search and filter articles
   - View full article details with images

### API Endpoints

#### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/auth/validate` - Validate token

#### Articles
- `POST /api/articles/add` - Add new article
- `GET /api/articles/all` - Get all articles
- `GET /api/articles/:title` - Get article by title

## Project Structure

```
Informate/
├── Backend/
│   └── informate/
│       ├── src/main/java/com/example/informate/
│       │   ├── main.java              # Main application entry point
│       │   ├── auth.java              # Authentication logic
│       │   ├── articles.java          # Article management
│       │   ├── scraper.java           # Web scraping functionality
│       │   ├── AI.java                # OpenAI integration
│       │   └── EnvLoader.java         # Environment variable loader
│       ├── pom.xml                    # Maven dependencies
│       └── env.template               # Environment template
├── Frontend/
│   └── informate-frontend/
│       ├── src/
│       │   ├── components/            # React components
│       │   ├── contexts/              # Context providers
│       │   └── App.js                 # Main app component
│       └── package.json               # Node.js dependencies
├── .gitignore                         # Git ignore rules
├── env.template                       # Environment template
└── README.md                          # This file
```

## Security Notes

- Environment variables (`.env` files) are excluded from version control
- Database files are not committed to prevent data leaks
- API keys and sensitive data should never be hardcoded

## Development

### Adding New Features

1. Backend changes go in `Backend/informate/src/main/java/com/example/informate/`
2. Frontend changes go in `Frontend/informate-frontend/src/`
3. Update API documentation when adding new endpoints

### Database Schema

The application uses SQLite with two main tables:
- `user` - User authentication data
- `articles` - Article content and metadata

## Troubleshooting

### Common Issues

1. **"OpenAI API key not found"**
   - Ensure `.env` files are created with valid API key
   - Check that the key starts with `sk-`

2. **"Database connection failed"**
   - Ensure SQLite is available
   - Check file permissions in project directory

3. **"Article scraping failed"**
   - Verify URL is accessible
   - Some websites may block automated access

4. **Frontend not connecting to backend**
   - Ensure backend is running on port 8080
   - Check CORS configuration

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is for educational purposes. Please respect website terms of service when scraping content.

## Contact

For questions or support, please contact: AGI105@student.aru.ac.uk 