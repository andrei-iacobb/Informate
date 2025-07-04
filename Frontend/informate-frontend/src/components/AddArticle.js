import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import axios from 'axios';

const AddArticle = () => {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [processingId, setProcessingId] = useState(null);
  const [status, setStatus] = useState('');
  const [progress, setProgress] = useState(0);
  const [success, setSuccess] = useState(false);
  const [failed, setFailed] = useState(false);
  
  const { currentUser, logout, API_BASE_URL } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setProcessingId(null);
    setStatus('');
    setProgress(0);
    setSuccess(false);
    setFailed(false);

    try {
      const response = await axios.post(`${API_BASE_URL}/articles`, {
        url: url
      });

      if (response.data.success) {
        setProcessingId(response.data.processingId);
        setStatus('Processing started...');
      } else {
        setError(response.data.message || 'Failed to start processing');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add article');
    } finally {
      setLoading(false);
    }
  };

  // Poll for processing status
  useEffect(() => {
    if (!processingId) return;

    const pollStatus = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/articles/status/${processingId}`);
        const data = response.data;

        setStatus(data.status || 'Processing...');
        setProgress(data.progress || 0);

        if (data.completed) {
          if (data.success) {
            setSuccess(true);
            setStatus('Article processed successfully!');
            setProgress(100);
          } else {
            setFailed(true);
            setStatus(data.error || 'Processing failed');
          }
        }
      } catch (err) {
        console.error('Error polling status:', err);
        // Continue polling even if there's an error
      }
    };

    const interval = setInterval(pollStatus, 2000);
    
    // Clean up interval
    return () => clearInterval(interval);
  }, [processingId, API_BASE_URL]);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const resetForm = () => {
    setUrl('');
    setLoading(false);
    setError('');
    setProcessingId(null);
    setStatus('');
    setProgress(0);
    setSuccess(false);
    setFailed(false);
  };

  const StatusIcon = () => {
    if (success) {
      return (
        <svg style={{ width: '32px', height: '32px', color: '#16a34a' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
        </svg>
      );
    }
    if (failed) {
      return (
        <svg style={{ width: '32px', height: '32px', color: '#dc2626' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
        </svg>
      );
    }
    return (
      <div className="spinner"></div>
    );
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="header-content">
          <div className="logo-section">
            <Link to="/dashboard" className="logo-section" style={{ color: 'inherit', textDecoration: 'none' }}>
              <div className="logo-icon">
                <span>I</span>
              </div>
              <h1 className="logo-text">Informate</h1>
            </Link>
          </div>
          
          <div className="nav-section">
            <span className="user-greeting">Welcome, {currentUser?.username}</span>
            <Link
              to="/dashboard"
              className="btn btn-ghost"
            >
              ← Dashboard
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
      <main className="content-wrapper" style={{ padding: '3rem 1.5rem' }}>
        <div className="text-center" style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1e293b', marginBottom: '0.5rem' }}>Add New Article</h2>
          <p style={{ color: '#64748b' }}>Paste any article URL to get an AI-powered summary with keywords</p>
        </div>

        <div className="card" style={{ maxWidth: '800px', margin: '0 auto', padding: '2rem' }}>
          {!processingId ? (
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="url" className="form-label" style={{ fontSize: '1.125rem', marginBottom: '0.75rem' }}>
                  Article URL
                </label>
                <input
                  id="url"
                  type="url"
                  required
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  className="form-input"
                  style={{ fontSize: '1.125rem', padding: '1rem' }}
                  placeholder="https://example.com/article"
                />
                <p className="form-help">
                  Enter the full URL of any news article, blog post, or web page you'd like to summarize.
                </p>
              </div>

              {error && (
                <div className="alert alert-error">
                  {error}
                </div>
              )}

              <div style={{ display: 'flex', gap: '1rem' }}>
                <button
                  type="submit"
                  disabled={loading || !url.trim()}
                  className="btn btn-primary"
                  style={{ flex: '1', padding: '0.75rem 1.5rem', fontSize: '1rem' }}
                >
                  {loading ? 'Starting...' : 'Process Article'}
                </button>
                <Link
                  to="/dashboard"
                  className="btn btn-secondary"
                  style={{ padding: '0.75rem 1.5rem', fontSize: '1rem' }}
                >
                  Cancel
                </Link>
              </div>
            </form>
          ) : (
            <div className="text-center" style={{ padding: '2rem 0' }}>
              <div style={{ marginBottom: '1.5rem' }}>
                <StatusIcon />
              </div>

              <h3 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#1e293b', marginBottom: '1rem' }}>
                {success ? 'Success!' : failed ? 'Processing Failed' : 'Processing Article'}
              </h3>

              <div style={{ marginBottom: '1.5rem' }}>
                <div style={{ fontSize: '1.125rem', color: '#64748b', marginBottom: '0.5rem' }}>{status}</div>
                {!success && !failed && (
                  <div className="progress-bar">
                    <div className="progress-fill" style={{
                      width: `${Math.min(progress, 100)}%`
                    }}></div>
                  </div>
                )}
              </div>

              {(success || failed) && (
                <div>
                  {success && (
                    <div className="success-message">
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1rem' }}>
                        <svg style={{ width: '24px', height: '24px', color: '#16a34a' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                      </div>
                      <h4 className="success-title">Article Added Successfully!</h4>
                      <p className="success-description">
                        Your article has been processed and added to your feed. You can view it in your dashboard.
                      </p>
                      <p className="success-note">
                        Processing completed with AI-generated summary and keywords.
                      </p>
                    </div>
                  )}

                  <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                    <Link
                      to="/dashboard"
                      className="btn btn-primary"
                    >
                      {success ? 'View Articles' : 'Back to Dashboard'}
                    </Link>
                    <button
                      onClick={resetForm}
                      className="btn btn-secondary"
                    >
                      Add Another
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Tips Section */}
        <div className="tips-section" style={{ marginTop: '3rem', maxWidth: '800px', marginLeft: 'auto', marginRight: 'auto' }}>
          <h3 className="tips-title">💡 Tips for Best Results</h3>
          <ul className="tips-list">
            <li className="tips-item">
              <span className="tips-bullet"></span>
              <span>Use direct links to articles, not homepage URLs</span>
            </li>
            <li className="tips-item">
              <span className="tips-bullet"></span>
              <span>Works best with news articles, blog posts, and research papers</span>
            </li>
            <li className="tips-item">
              <span className="tips-bullet"></span>
              <span>Processing typically takes 30-60 seconds depending on article length</span>
            </li>
            <li className="tips-item">
              <span className="tips-bullet"></span>
              <span>Make sure the URL is publicly accessible (not behind paywall)</span>
            </li>
          </ul>
        </div>
      </main>
    </div>
  );
};

export default AddArticle; 