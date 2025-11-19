import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
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
  const { darkMode, toggleDarkMode } = useTheme();
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
        <div className="w-16 h-16 bg-green-100 dark:bg-green-900/30 rounded-full flex items-center justify-center mx-auto">
          <svg className="w-8 h-8 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>
      );
    }
    if (failed) {
      return (
        <div className="w-16 h-16 bg-red-100 dark:bg-red-900/30 rounded-full flex items-center justify-center mx-auto">
          <svg className="w-8 h-8 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
      );
    }
    return (
      <svg className="animate-spin h-16 w-16 text-blue-600 dark:text-blue-400 mx-auto" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    );
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors duration-300">
      {/* Header */}
      <header className="bg-white dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 sticky top-0 z-50 transition-colors duration-300 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <Link to="/dashboard" className="flex items-center gap-3 hover:opacity-80 transition-opacity">
              <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-blue-600 dark:from-blue-600 dark:to-blue-700 rounded-lg flex items-center justify-center shadow-lg">
                <span className="text-white font-bold text-lg">I</span>
              </div>
              <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Informate</h1>
            </Link>

            <div className="flex items-center gap-3">
              <span className="hidden sm:block text-sm text-slate-600 dark:text-slate-400">
                Welcome, <span className="font-medium text-slate-900 dark:text-white">{currentUser?.username}</span>
              </span>

              <button
                onClick={toggleDarkMode}
                className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
                aria-label="Toggle dark mode"
              >
                {darkMode ? (
                  <svg className="w-5 h-5 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                  </svg>
                ) : (
                  <svg className="w-5 h-5 text-slate-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                  </svg>
                )}
              </button>

              <Link
                to="/dashboard"
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
                <span className="hidden sm:inline">Dashboard</span>
              </Link>

              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center mb-10">
          <h2 className="text-3xl font-bold text-slate-900 dark:text-white mb-3">Add New Article</h2>
          <p className="text-lg text-slate-600 dark:text-slate-400">Paste any article URL to get an AI-powered summary with keywords</p>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl p-8 border border-slate-200 dark:border-slate-700 transition-colors duration-300">
          {!processingId ? (
            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label htmlFor="url" className="block text-lg font-semibold text-slate-900 dark:text-white mb-3">
                  Article URL
                </label>
                <input
                  id="url"
                  type="url"
                  required
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  className="w-full px-5 py-4 text-lg bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 focus:border-transparent transition-all text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500"
                  placeholder="https://example.com/article"
                />
                <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
                  Enter the full URL of any news article, blog post, or web page you'd like to summarize.
                </p>
              </div>

              {error && (
                <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
                  <p className="text-sm text-red-800 dark:text-red-200">{error}</p>
                </div>
              )}

              <div className="flex gap-4">
                <button
                  type="submit"
                  disabled={loading || !url.trim()}
                  className="flex-1 py-4 px-6 text-base font-semibold text-white bg-blue-600 hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600 rounded-lg shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                >
                  {loading ? 'Starting...' : 'Process Article'}
                </button>
                <Link
                  to="/dashboard"
                  className="py-4 px-6 text-base font-medium text-slate-700 dark:text-slate-300 bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 rounded-lg transition-colors duration-200 text-center"
                >
                  Cancel
                </Link>
              </div>
            </form>
          ) : (
            <div className="text-center py-8">
              <div className="mb-6">
                <StatusIcon />
              </div>

              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mb-4">
                {success ? 'Success!' : failed ? 'Processing Failed' : 'Processing Article'}
              </h3>

              <div className="mb-8">
                <div className="text-lg text-slate-600 dark:text-slate-400 mb-3">{status}</div>
                {!success && !failed && (
                  <div className="w-full max-w-md mx-auto h-2 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-blue-500 to-blue-600 dark:from-blue-400 dark:to-blue-500 rounded-full transition-all duration-300"
                      style={{ width: `${Math.min(progress, 100)}%` }}
                    ></div>
                  </div>
                )}
              </div>

              {(success || failed) && (
                <div>
                  {success && (
                    <div className="mb-8 p-6 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
                      <div className="flex items-center justify-center mb-3">
                        <svg className="w-6 h-6 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                      </div>
                      <h4 className="text-lg font-semibold text-green-900 dark:text-green-100 mb-2">Article Added Successfully!</h4>
                      <p className="text-green-800 dark:text-green-200 mb-2">
                        Your article has been processed and added to your feed. You can view it in your dashboard.
                      </p>
                      <p className="text-sm text-green-700 dark:text-green-300">
                        Processing completed with AI-generated summary and keywords.
                      </p>
                    </div>
                  )}

                  <div className="flex gap-4 justify-center">
                    <Link
                      to="/dashboard"
                      className="px-8 py-3 text-base font-semibold text-white bg-blue-600 hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600 rounded-lg shadow-lg hover:shadow-xl transform hover:-translate-y-1 transition-all duration-200"
                    >
                      {success ? 'View Articles' : 'Back to Dashboard'}
                    </Link>
                    <button
                      onClick={resetForm}
                      className="px-8 py-3 text-base font-medium text-slate-700 dark:text-slate-300 bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 rounded-lg transition-colors duration-200"
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
        <div className="mt-10 bg-blue-50 dark:bg-blue-900/20 rounded-2xl p-8 border border-blue-200 dark:border-blue-800 transition-colors duration-300">
          <h3 className="text-lg font-semibold text-blue-900 dark:text-blue-100 mb-4 flex items-center gap-2">
            <span>💡</span> Tips for Best Results
          </h3>
          <ul className="space-y-3">
            <li className="flex items-start gap-3 text-blue-800 dark:text-blue-200">
              <span className="w-2 h-2 bg-blue-600 dark:bg-blue-400 rounded-full mt-2 flex-shrink-0"></span>
              <span>Use direct links to articles, not homepage URLs</span>
            </li>
            <li className="flex items-start gap-3 text-blue-800 dark:text-blue-200">
              <span className="w-2 h-2 bg-blue-600 dark:bg-blue-400 rounded-full mt-2 flex-shrink-0"></span>
              <span>Works best with news articles, blog posts, and research papers</span>
            </li>
            <li className="flex items-start gap-3 text-blue-800 dark:text-blue-200">
              <span className="w-2 h-2 bg-blue-600 dark:bg-blue-400 rounded-full mt-2 flex-shrink-0"></span>
              <span>Processing typically takes 30-60 seconds depending on article length</span>
            </li>
            <li className="flex items-start gap-3 text-blue-800 dark:text-blue-200">
              <span className="w-2 h-2 bg-blue-600 dark:bg-blue-400 rounded-full mt-2 flex-shrink-0"></span>
              <span>Make sure the URL is publicly accessible (not behind paywall)</span>
            </li>
          </ul>
        </div>
      </main>
    </div>
  );
};

export default AddArticle;
