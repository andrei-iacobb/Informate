import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import axios from 'axios';

const StackSuggestionModal = ({ articleId, onClose }) => {
  const [suggestions, setSuggestions] = useState(null);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const { API_BASE_URL } = useAuth();
  const navigate = useNavigate();

  React.useEffect(() => {
    const fetchSuggestions = async () => {
      try {
        const response = await axios.post(`${API_BASE_URL}/stacks/suggest`, { articleId });
        setSuggestions(response.data);
        setNewName(response.data.suggestedName || 'New Stack');
      } catch (err) {
        console.error('Error fetching suggestions:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchSuggestions();
  }, [API_BASE_URL, articleId]);

  const handleAddToStack = async (stackId) => {
    try {
      await axios.post(`${API_BASE_URL}/stacks/${stackId}/articles`, { articleId });
      navigate(`/stack/${stackId}`);
    } catch (err) {
      console.error('Error adding to stack:', err);
    }
  };

  const handleCreateStack = async () => {
    setCreating(true);
    try {
      const response = await axios.post(`${API_BASE_URL}/stacks`, { name: newName, keywords: '' });
      if (response.data.success) {
        await axios.post(`${API_BASE_URL}/stacks/${response.data.id}/articles`, { articleId });
        navigate(`/stack/${response.data.id}`);
      }
    } catch (err) {
      console.error('Error creating stack:', err);
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl p-6 w-full max-w-md border border-slate-200 dark:border-slate-700">
        <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-2">Add to Analysis Stack?</h3>
        <p className="text-sm text-slate-500 dark:text-slate-400 mb-4">Group this article with related news for deep analysis.</p>

        {loading ? (
          <div className="py-8 text-center">
            <svg className="animate-spin h-8 w-8 text-blue-600 mx-auto" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
          </div>
        ) : (
          <div className="space-y-3">
            {suggestions?.existingStacks?.length > 0 && (
              <div>
                <p className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Matching Stacks:</p>
                {suggestions.existingStacks.map((stack) => (
                  <button key={stack.id} onClick={() => handleAddToStack(stack.id)} className="w-full text-left p-3 mb-2 bg-blue-50 dark:bg-blue-900/20 hover:bg-blue-100 dark:hover:bg-blue-900/40 border border-blue-200 dark:border-blue-800 rounded-lg transition-colors">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium text-slate-900 dark:text-white">{stack.name}</span>
                      <span className="text-xs text-blue-600 dark:text-blue-400">{stack.matchScore}% match</span>
                    </div>
                  </button>
                ))}
              </div>
            )}

            {!showCreate ? (
              <button onClick={() => setShowCreate(true)} className="w-full py-2 px-4 text-sm font-medium text-purple-700 dark:text-purple-300 bg-purple-50 dark:bg-purple-900/20 hover:bg-purple-100 dark:hover:bg-purple-900/40 border border-purple-200 dark:border-purple-800 rounded-lg transition-colors">
                Create New Stack
              </button>
            ) : (
              <div className="p-3 bg-slate-50 dark:bg-slate-900 rounded-lg">
                <input type="text" value={newName} onChange={(e) => setNewName(e.target.value)} className="w-full px-3 py-2 mb-2 text-sm bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-lg text-slate-900 dark:text-white" />
                <button onClick={handleCreateStack} disabled={creating || !newName.trim()} className="w-full py-2 text-sm font-medium text-white bg-purple-600 hover:bg-purple-700 rounded-lg disabled:opacity-50">
                  {creating ? 'Creating...' : 'Create & Add'}
                </button>
              </div>
            )}

            <button onClick={onClose} className="w-full py-2 text-sm text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 transition-colors">
              Skip for now
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default StackSuggestionModal;
