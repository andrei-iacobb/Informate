import React, { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import axios from 'axios';

const ArticleView = () => {
  const { id } = useParams();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { API_BASE_URL, loading: authLoading } = useAuth();
  const { darkMode } = useTheme();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchArticle = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/articles`);
        const articles = response.data.articles || [];
        const found = articles.find((a, idx) => idx === parseInt(id) || a.id === parseInt(id));
        if (found) {
          setArticle(found);
        } else {
          setError('Article not found');
        }
      } catch (err) {
        setError('Failed to load article');
        console.error('Error fetching article:', err);
      } finally {
        setLoading(false);
      }
    };

    if (!authLoading) {
      fetchArticle();
    }
  }, [id, API_BASE_URL, authLoading]);

  const getArticleImage = (article) => {
    if (!article?.images) return null;
    const imageFiles = article.images.split(',').map(img => img.trim()).filter(img => img.length > 0);
    if (imageFiles.length === 0) return null;
    return `/SiteImages/${imageFiles[0]}`;
  };

  const parseKeywords = (keywordsString) => {
    if (!keywordsString) return [];
    return keywordsString.split(',').map(k => k.trim()).filter(k => k.length > 0);
  };

  if (loading || authLoading) {
    return (
      <div className={`min-h-screen flex items-center justify-center ${darkMode ? 'bg-slate-900' : 'bg-slate-50'}`}>
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-blue-500 border-t-transparent"></div>
      </div>
    );
  }

  if (error || !article) {
    return (
      <div className={`min-h-screen flex flex-col items-center justify-center ${darkMode ? 'bg-slate-900' : 'bg-slate-50'}`}>
        <p className="text-red-500 mb-4">{error || 'Article not found'}</p>
        <Link to="/dashboard" className="text-blue-500 hover:underline">Back to Dashboard</Link>
      </div>
    );
  }

  const imageUrl = getArticleImage(article);
  const keywords = parseKeywords(article.keywords);

  return (
    <div className={`min-h-screen ${darkMode ? 'bg-slate-900' : 'bg-slate-50'}`}>
      {/* Header */}
      <header className={`sticky top-0 z-50 backdrop-blur-xl ${darkMode ? 'bg-slate-900/80 border-slate-700' : 'bg-white/80 border-slate-200'} border-b`}>
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <button
            onClick={() => navigate('/dashboard')}
            className={`flex items-center gap-2 ${darkMode ? 'text-slate-300 hover:text-white' : 'text-slate-600 hover:text-slate-900'} transition-colors`}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to Dashboard
          </button>
        </div>
      </header>

      {/* Article Content */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <article className={`${darkMode ? 'bg-slate-800' : 'bg-white'} rounded-2xl shadow-xl overflow-hidden`}>
          {/* Image */}
          {imageUrl && (
            <div className="aspect-video w-full overflow-hidden">
              <img
                src={imageUrl}
                alt={article.title || 'Article image'}
                className="w-full h-full object-cover"
                onError={(e) => { e.target.style.display = 'none'; }}
              />
            </div>
          )}

          <div className="p-8">
            {/* Title */}
            <h1 className={`text-3xl font-bold mb-6 ${darkMode ? 'text-white' : 'text-slate-900'}`}>
              {article.title || 'Untitled Article'}
            </h1>

            {/* Keywords */}
            {keywords.length > 0 && (
              <div className="flex flex-wrap gap-2 mb-6">
                {keywords.map((keyword, idx) => (
                  <span
                    key={idx}
                    className="px-3 py-1 text-sm font-medium bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded-full"
                  >
                    {keyword}
                  </span>
                ))}
              </div>
            )}

            {/* Summary */}
            <div className={`prose max-w-none ${darkMode ? 'prose-invert' : ''}`}>
              <h2 className={`text-xl font-semibold mb-4 ${darkMode ? 'text-slate-200' : 'text-slate-700'}`}>Summary</h2>
              <p className={`text-lg leading-relaxed whitespace-pre-wrap ${darkMode ? 'text-slate-300' : 'text-slate-600'}`}>
                {article.summary || 'Summary not available yet. The article is still being processed.'}
              </p>
            </div>

            {/* Original URL */}
            {article.url && (
              <div className="mt-8 pt-6 border-t border-slate-200 dark:border-slate-700">
                <a
                  href={article.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 text-blue-600 dark:text-blue-400 hover:underline"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                  </svg>
                  View Original Article
                </a>
              </div>
            )}
          </div>
        </article>
      </main>
    </div>
  );
};

export default ArticleView;
