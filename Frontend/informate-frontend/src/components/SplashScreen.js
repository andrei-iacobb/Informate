import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const SplashScreen = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  // Redirect to dashboard if already authenticated
  React.useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  const demoArticles = [
    {
      title: "Breaking: AI Revolution in Tech Industry",
      summary: "New developments in artificial intelligence are transforming how we work and live...",
      keywords: ["AI", "Technology", "Innovation", "Future"],
      image: "https://via.placeholder.com/200x120/3b82f6/white?text=AI+News"
    },
    {
      title: "Climate Change: Scientists Discover New Solutions",
      summary: "Breakthrough research reveals promising approaches to combat global warming...",
      keywords: ["Climate", "Science", "Environment", "Research"],
      image: "https://via.placeholder.com/200x120/10b981/white?text=Climate"
    },
    {
      title: "Space Exploration: Mars Mission Updates",
      summary: "Latest mission to Mars provides unprecedented insights into the red planet...",
      keywords: ["Space", "Mars", "NASA", "Discovery"],
      image: "https://via.placeholder.com/200x120/f59e0b/white?text=Space"
    }
  ];

  return (
    <div className="splash-container">
      {/* Header */}
      <header className="splash-header">
        <div className="header-content">
          <div className="logo-section">
            <div className="logo-icon">
              <span>I</span>
            </div>
            <h1 className="logo-text">Informate</h1>
          </div>
          <div className="nav-section">
            <Link
              to="/login"
              className="btn btn-ghost"
            >
              Login
            </Link>
            <Link
              to="/register"
              className="btn btn-primary"
            >
              Sign Up
            </Link>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="splash-main">
        {/* Hero Section */}
        <div className="hero-section">
          <h2 className="hero-title">
            Stay Informed with
            <span className="hero-accent"> AI-Powered</span> News
          </h2>
          <p className="hero-subtitle">
            Transform any article into concise summaries with intelligent keyword extraction. 
            Never miss important news again.
          </p>
          <div className="hero-buttons">
            <Link
              to="/register"
              className="btn btn-primary btn-large"
            >
              Get Started Free
            </Link>
            <button className="btn btn-secondary btn-large">
              Learn More
            </button>
          </div>
        </div>

        {/* Features Section */}
        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon">
              <svg style={{ width: '24px', height: '24px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h3 className="feature-title">Lightning Fast</h3>
            <p className="feature-description">Process articles in seconds with our advanced AI technology</p>
          </div>
          
          <div className="feature-card">
            <div className="feature-icon">
              <svg style={{ width: '24px', height: '24px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 className="feature-title">Smart Summaries</h3>
            <p className="feature-description">Get the key points without reading entire articles</p>
          </div>
          
          <div className="feature-card">
            <div className="feature-icon">
              <svg style={{ width: '24px', height: '24px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.99 1.99 0 013 12V7a4 4 0 014-4z" />
              </svg>
            </div>
            <h3 className="feature-title">Keyword Extraction</h3>
            <p className="feature-description">Automatically identify important topics and themes</p>
          </div>
        </div>

        {/* Demo Section */}
        <div className="demo-section">
          <h3 className="demo-title">See How It Works</h3>
          <div className="demo-grid">
            {demoArticles.map((article, index) => (
              <div key={index} className="demo-article">
                <img 
                  src={article.image} 
                  alt={article.title}
                  className="demo-image"
                />
                <div className="card-body">
                  <h4 className="article-title" style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>{article.title}</h4>
                  <p className="article-summary" style={{ fontSize: '12px', marginBottom: '0.75rem' }}>{article.summary}</p>
                  <div className="keyword-container">
                    {article.keywords.slice(0, 3).map((keyword, idx) => (
                      <span 
                        key={idx}
                        className="keyword-tag"
                        style={{ fontSize: '10px' }}
                      >
                        {keyword}
                      </span>
                    ))}
                    {article.keywords.length > 3 && (
                      <span className="keyword-overflow" style={{ fontSize: '10px' }}>
                        +{article.keywords.length - 3}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="text-center" style={{ marginTop: '2rem' }}>
            <Link
              to="/register"
              className="btn btn-primary"
              style={{ display: 'inline-flex', alignItems: 'center' }}
            >
              Start Creating Your Feed
              <svg style={{ marginLeft: '0.5rem', width: '16px', height: '16px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </Link>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="app-footer">
        <div className="footer-content">
          <div className="footer-logo">
            <div className="footer-logo-icon">
              <span>I</span>
            </div>
            <span className="footer-text">Informate</span>
          </div>
          <p className="footer-description">Transform how you consume news with AI-powered insights.</p>
        </div>
      </footer>
    </div>
  );
};

export default SplashScreen; 