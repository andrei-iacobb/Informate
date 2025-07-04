import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import axios from 'axios';

const Dashboard = () => {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { currentUser, logout, API_BASE_URL } = useAuth();
  const navigate = useNavigate();

  const fetchArticles = useCallback(async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/articles`);
      setArticles(response.data.articles || []);
    } catch (err) {
      setError('Failed to load articles');
      console.error('Error fetching articles:', err);
    } finally {
      setLoading(false);
    }
  }, [API_BASE_URL]);

  useEffect(() => {
    fetchArticles();
  }, [fetchArticles]);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const truncateText = (text, maxLength = 120) => {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  };

  const parseKeywords = (keywordsString) => {
    if (!keywordsString) return [];
    return keywordsString.split(',').map(k => k.trim()).filter(k => k.length > 0);
  };

  const getDisplayKeywords = (keywords, maxKeywords = 4) => {
    if (keywords.length <= maxKeywords) {
      return { displayed: keywords, hidden: 0 };
    }
    return {
      displayed: keywords.slice(0, maxKeywords),
      hidden: keywords.length - maxKeywords
    };
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-content">
          <div className="spinner"></div>
          <p>Loading your articles...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="header-content">
          <Link to="/dashboard" className="logo-section">
            <div className="logo-icon">
              <span>I</span>
            </div>
            <h1 className="logo-text">Informate</h1>
          </Link>
          
          <div className="nav-section">
            <span className="user-greeting">Welcome, {currentUser?.username}</span>
            <Link
              to="/add-article"
              className="btn btn-primary"
            >
              <svg style={{ width: '16px', height: '16px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              <span>Add Article</span>
            </Link>
            <button
              onClick={handleLogout}
              className="btn btn-ghost"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="content-wrapper" style={{ padding: '2rem 1.5rem' }}>
        <div style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1e293b', marginBottom: '0.5rem' }}>Your News Feed</h2>
          <p style={{ color: '#64748b' }}>Stay updated with your personalized AI-summarized articles</p>
        </div>

        {error && (
          <div className="alert alert-error">
            {error}
          </div>
        )}

        {articles.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">
              <svg style={{ width: '48px', height: '48px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
              </svg>
            </div>
            <h3 className="empty-title">No articles yet</h3>
            <p className="empty-description">Start building your personalized news feed by adding your first article.</p>
            <Link
              to="/add-article"
              className="btn btn-primary"
            >
              <svg style={{ width: '20px', height: '20px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              Add Your First Article
            </Link>
          </div>
        ) : (
          <div className="articles-grid">
            {articles.map((article, index) => {
              const keywords = parseKeywords(article.keywords);
              const { displayed: displayKeywords, hidden: hiddenCount } = getDisplayKeywords(keywords);
              
              return (
                <div
                  key={index}
                  className="article-card"
                >
                  <div className="card-body">
                    {/* Title */}
                    <h3 className="article-title">
                      {article.title || 'Untitled Article'}
                    </h3>
                    
                    {/* Summary */}
                    <p className="article-summary">
                      {truncateText(article.summary || 'Summary not available yet.')}
                    </p>
                    
                    {/* Keywords */}
                    <div className="keyword-container">
                      {displayKeywords.map((keyword, idx) => (
                        <span
                          key={idx}
                          className="keyword-tag"
                        >
                          {keyword}
                        </span>
                      ))}
                      {hiddenCount > 0 && (
                        <span className="keyword-overflow">
                          +{hiddenCount}
                        </span>
                      )}
                    </div>
                    
                    {/* Processing Status */}
                    {!article.summary && (
                      <div className="processing-indicator">
                        <div className="spinner spinner-small" style={{ marginRight: '0.5rem' }}></div>
                        <span>Processing...</span>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Refresh Button */}
        {articles.length > 0 && (
          <div className="text-center" style={{ marginTop: '3rem' }}>
            <button
              onClick={fetchArticles}
              className="btn btn-secondary"
            >
              <svg style={{ width: '16px', height: '16px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              <span>Refresh Feed</span>
            </button>
          </div>
        )}
      </main>
    </div>
  );
};

export default Dashboard; 