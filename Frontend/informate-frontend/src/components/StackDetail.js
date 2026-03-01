import React, { useState, useEffect, useCallback } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import axios from 'axios';

const StackDetail = () => {
  const { id } = useParams();
  const [stack, setStack] = useState(null);
  const [loading, setLoading] = useState(true);
  const [focus, setFocus] = useState('');
  const [searchDepth, setSearchDepth] = useState(10);
  const [analyzing, setAnalyzing] = useState(false);
  const [allArticles, setAllArticles] = useState([]);
  const [showAddArticle, setShowAddArticle] = useState(false);

  const { API_BASE_URL, loading: authLoading, logout } = useAuth();
  const { darkMode, toggleDarkMode } = useTheme();
  const navigate = useNavigate();

  const fetchStack = useCallback(async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/stacks/${id}`);
      setStack(response.data);
      setFocus(response.data.focus || '');
      setSearchDepth(response.data.searchDepth || 10);
    } catch (err) {
      console.error('Error fetching stack:', err);
    } finally {
      setLoading(false);
    }
  }, [API_BASE_URL, id]);

  useEffect(() => {
    if (!authLoading) fetchStack();
  }, [fetchStack, authLoading]);

  // Poll status when analyzing
  useEffect(() => {
    if (!stack || (stack.status !== 'searching' && stack.status !== 'analyzing')) return;
    const interval = setInterval(async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/stacks/${id}/status`);
        if (response.data.status === 'ready' || response.data.status === 'error') {
          setAnalyzing(false);
          fetchStack();
          clearInterval(interval);
        }
      } catch (err) {
        console.error('Error polling status:', err);
      }
    }, 3000);
    return () => clearInterval(interval);
  }, [stack, API_BASE_URL, id, fetchStack]);

  const handleAnalyze = async () => {
    setAnalyzing(true);
    try {
      await axios.put(`${API_BASE_URL}/stacks/${id}`, { focus, searchDepth });
      await axios.post(`${API_BASE_URL}/stacks/${id}/analyze`);
      fetchStack();
    } catch (err) {
      console.error('Error starting analysis:', err);
      setAnalyzing(false);
    }
  };

  const handleAddArticle = async (articleId) => {
    try {
      await axios.post(`${API_BASE_URL}/stacks/${id}/articles`, { articleId });
      setShowAddArticle(false);
      fetchStack();
    } catch (err) {
      console.error('Error adding article:', err);
    }
  };

  const handleRemoveArticle = async (articleId) => {
    try {
      await axios.delete(`${API_BASE_URL}/stacks/${id}/articles/${articleId}`);
      fetchStack();
    } catch (err) {
      console.error('Error removing article:', err);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Delete this stack and its analysis?')) return;
    try {
      await axios.delete(`${API_BASE_URL}/stacks/${id}`);
      navigate('/stacks');
    } catch (err) {
      console.error('Error deleting stack:', err);
    }
  };

  const fetchAllArticles = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/articles`);
      setAllArticles(response.data.articles || []);
      setShowAddArticle(true);
    } catch (err) {
      console.error('Error fetching articles:', err);
    }
  };

  const probColor = { Low: 'bg-amber-500', Medium: 'bg-orange-500', High: 'bg-red-500' };
  const probWidth = { Low: 'w-1/4', Medium: 'w-1/2', High: 'w-3/4' };

  const statusColors = {
    pending: 'bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300',
    searching: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
    analyzing: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300',
    ready: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
    error: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center">
        <svg className="animate-spin h-12 w-12 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      </div>
    );
  }

  if (!stack) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center">
        <p className="text-slate-600 dark:text-slate-400">Stack not found.</p>
      </div>
    );
  }

  let analysis = null;
  try {
    analysis = stack.analysis ? JSON.parse(stack.analysis) : null;
  } catch (e) {
    console.error('Failed to parse analysis:', e);
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 transition-colors duration-300">
      {/* Header */}
      <header className="bg-white dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 sticky top-0 z-50 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-4">
              <Link to="/stacks" className="p-2 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors">
                <svg className="w-5 h-5 text-slate-600 dark:text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
              </Link>
              <h1 className="text-xl font-bold text-slate-900 dark:text-white truncate">{stack.name}</h1>
              <span className={`px-2 py-1 text-xs font-medium rounded-full ${statusColors[stack.status] || statusColors.pending}`}>
                {stack.status}
              </span>
            </div>
            <div className="flex items-center gap-3">
              <button onClick={toggleDarkMode} className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors">
                {darkMode ? (
                  <svg className="w-5 h-5 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
                ) : (
                  <svg className="w-5 h-5 text-slate-700" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>
                )}
              </button>
              <button onClick={handleDelete} className="p-2 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Controls */}
        <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Focus Area</label>
              <input type="text" value={focus} onChange={(e) => setFocus(e.target.value)} className="w-full px-4 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-300 dark:border-slate-600 rounded-lg text-slate-900 dark:text-white" placeholder="e.g., stock market impact" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Search Depth: {searchDepth}</label>
              <input type="range" min="5" max="30" value={searchDepth} onChange={(e) => setSearchDepth(parseInt(e.target.value))} className="w-full" />
            </div>
            <button onClick={handleAnalyze} disabled={analyzing || stack.status === 'searching' || stack.status === 'analyzing'} className="py-2 px-6 text-sm font-medium text-white bg-purple-600 hover:bg-purple-700 rounded-lg disabled:opacity-50 transition-colors">
              {analyzing || stack.status === 'searching' || stack.status === 'analyzing' ? 'Analyzing...' : analysis ? 'Re-Analyze' : 'Analyze'}
            </button>
          </div>
        </div>

        <div className="flex flex-col lg:flex-row gap-6">
          {/* Analysis Panel */}
          <div className="flex-1 min-w-0">
            {!analysis ? (
              <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-12 text-center">
                {stack.status === 'searching' || stack.status === 'analyzing' ? (
                  <div>
                    <svg className="animate-spin h-12 w-12 text-purple-600 mx-auto mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                    <p className="text-lg font-medium text-slate-900 dark:text-white">Analysis in progress...</p>
                    <p className="text-slate-500 mt-2">Status: {stack.status}</p>
                  </div>
                ) : (
                  <div>
                    <p className="text-lg text-slate-600 dark:text-slate-400 mb-4">Add articles and click Analyze to generate an intelligence briefing.</p>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-6">
                {/* Executive Summary */}
                <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
                    <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                    Executive Summary
                  </h3>
                  <p className="text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-line">{analysis.executive_summary}</p>
                </div>

                {/* Key Facts */}
                {analysis.key_facts && analysis.key_facts.length > 0 && (
                  <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
                    <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4">Key Facts</h3>
                    <ul className="space-y-2">
                      {analysis.key_facts.map((fact, i) => (
                        <li key={i} className="flex items-start gap-3 text-slate-700 dark:text-slate-300">
                          <span className="w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0"></span>
                          <span>{fact}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Perspectives */}
                {analysis.perspectives && analysis.perspectives.length > 0 && (
                  <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
                    <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4">Multiple Perspectives</h3>
                    <div className="space-y-4">
                      {analysis.perspectives.map((p, i) => (
                        <div key={i} className="pl-4 border-l-4 border-purple-500">
                          <h4 className="font-semibold text-slate-900 dark:text-white">{p.viewpoint}</h4>
                          <p className="text-slate-600 dark:text-slate-400 mt-1">{p.description}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Market Impact */}
                {analysis.market_impact && (
                  <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
                    <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4">Market & Economic Impact</h3>
                    <p className="text-slate-700 dark:text-slate-300 mb-4">{analysis.market_impact.summary}</p>
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                      {analysis.market_impact.sectors && analysis.market_impact.sectors.length > 0 && (
                        <div>
                          <h4 className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">Sectors</h4>
                          <div className="flex flex-wrap gap-1">{analysis.market_impact.sectors.map((s, i) => (
                            <span key={i} className="px-2 py-1 text-xs bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 rounded-full">{s}</span>
                          ))}</div>
                        </div>
                      )}
                      {analysis.market_impact.tickers && analysis.market_impact.tickers.length > 0 && (
                        <div>
                          <h4 className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">Tickers</h4>
                          <div className="flex flex-wrap gap-1">{analysis.market_impact.tickers.map((t, i) => (
                            <span key={i} className="px-2 py-1 text-xs bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded-full font-mono">{t}</span>
                          ))}</div>
                        </div>
                      )}
                      {analysis.market_impact.outlook && (
                        <div>
                          <h4 className="text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">Outlook</h4>
                          <p className="text-sm text-slate-700 dark:text-slate-300">{analysis.market_impact.outlook}</p>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Future Scenarios */}
                {analysis.future_scenarios && analysis.future_scenarios.length > 0 && (
                  <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
                    <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4">Future Scenarios</h3>
                    <div className="space-y-4">
                      {analysis.future_scenarios.map((s, i) => (
                        <div key={i} className="bg-slate-50 dark:bg-slate-900 rounded-lg p-4">
                          <div className="flex items-center justify-between mb-2">
                            <h4 className="font-semibold text-slate-900 dark:text-white">{s.scenario}</h4>
                            <span className={`px-2 py-1 text-xs font-bold rounded ${s.probability === 'High' ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' : s.probability === 'Medium' ? 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300' : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'}`}>{s.probability}</span>
                          </div>
                          <div className="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-full mb-3">
                            <div className={`h-full rounded-full ${probColor[s.probability] || 'bg-slate-400'} ${probWidth[s.probability] || 'w-1/4'}`}></div>
                          </div>
                          <p className="text-sm text-slate-600 dark:text-slate-400 mb-2">{s.reasoning}</p>
                          {s.timeline && <p className="text-xs text-slate-500">Timeline: {s.timeline}</p>}
                          {s.indicators && s.indicators.length > 0 && (
                            <div className="mt-2">
                              <p className="text-xs font-medium text-slate-500 mb-1">Watch for:</p>
                              <ul className="text-xs text-slate-500 list-disc list-inside">{s.indicators.map((ind, j) => <li key={j}>{ind}</li>)}</ul>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Sources */}
                {analysis.sources && analysis.sources.length > 0 && (
                  <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6">
                    <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4">Sources</h3>
                    <ul className="space-y-2">
                      {analysis.sources.map((s, i) => (
                        <li key={i}>
                          <a href={s.url} target="_blank" rel="noopener noreferrer" className="text-blue-600 dark:text-blue-400 hover:underline text-sm">{s.title || s.url}</a>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Articles Sidebar */}
          <div className="lg:w-80 flex-shrink-0">
            <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-6 sticky top-24">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">Articles</h3>
                <button onClick={fetchAllArticles} className="p-1.5 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-colors">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
                </button>
              </div>

              {stack.articles && stack.articles.length > 0 ? (
                <div className="space-y-3 max-h-96 overflow-y-auto">
                  {stack.articles.map((article) => (
                    <div key={article.id} className="flex items-start justify-between gap-2 p-3 bg-slate-50 dark:bg-slate-900 rounded-lg">
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-slate-900 dark:text-white truncate">{article.title}</p>
                        <span className={`text-xs ${article.source === 'manual' ? 'text-blue-600 dark:text-blue-400' : 'text-green-600 dark:text-green-400'}`}>{article.source === 'manual' ? 'Manual' : 'AI-found'}</span>
                      </div>
                      <button onClick={() => handleRemoveArticle(article.id)} className="p-1 text-red-400 hover:text-red-600 flex-shrink-0">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-slate-500 dark:text-slate-400">No articles added yet.</p>
              )}
            </div>

            {/* Add Article Modal */}
            {showAddArticle && (
              <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl p-6 w-full max-w-lg border border-slate-200 dark:border-slate-700 max-h-[80vh] overflow-y-auto">
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-4">Add Article to Stack</h3>
                  {allArticles.length === 0 ? (
                    <p className="text-slate-500">No articles available.</p>
                  ) : (
                    <div className="space-y-2">
                      {allArticles.filter(a => !stack.articles?.some(sa => String(sa.id) === a.id)).map((article) => (
                        <button key={article.id} onClick={() => handleAddArticle(parseInt(article.id))} className="w-full text-left p-3 bg-slate-50 dark:bg-slate-900 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-colors">
                          <p className="text-sm font-medium text-slate-900 dark:text-white truncate">{article.title}</p>
                          {article.keywords && <p className="text-xs text-slate-500 truncate mt-1">{article.keywords}</p>}
                        </button>
                      ))}
                    </div>
                  )}
                  <button onClick={() => setShowAddArticle(false)} className="mt-4 w-full py-2 text-sm font-medium text-slate-700 dark:text-slate-300 bg-slate-100 dark:bg-slate-700 rounded-lg">Close</button>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

export default StackDetail;
