import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Login = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError(''); // Clear error when user types
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const result = await login(formData.username, formData.password);
      if (result.success) {
        navigate('/dashboard');
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError('An unexpected error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        {/* Header */}
        <div className="text-center">
          <div className="logo-section" style={{ justifyContent: 'center', marginBottom: '1.5rem' }}>
            <div className="logo-icon">
              <span>I</span>
            </div>
            <h1 className="logo-text">Informate</h1>
          </div>
          <h2 className="hero-title" style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>Welcome back</h2>
          <p className="hero-subtitle" style={{ fontSize: '14px', marginBottom: '2rem' }}>Sign in to your account to continue</p>
        </div>

        {/* Login Form */}
        <div>
          {error && (
            <div className="alert alert-error">
              {error}
            </div>
          )}
          
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="username" className="form-label">
                Username
              </label>
              <input
                id="username"
                name="username"
                type="text"
                required
                value={formData.username}
                onChange={handleChange}
                className="form-input"
                placeholder="Enter your username"
              />
            </div>

            <div className="form-group">
              <label htmlFor="password" className="form-label">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                value={formData.password}
                onChange={handleChange}
                className="form-input"
                placeholder="Enter your password"
              />
            </div>

            <div className="form-group">
              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary"
                style={{ width: '100%' }}
              >
                {loading ? (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <div className="spinner spinner-small" style={{ marginRight: '0.5rem' }}></div>
                    Signing in...
                  </div>
                ) : (
                  'Sign in'
                )}
              </button>
            </div>
          </form>

          <div style={{ marginTop: '1.5rem' }}>
            <div style={{ position: 'relative' }}>
              <div style={{ position: 'absolute', inset: '0', display: 'flex', alignItems: 'center' }}>
                <div style={{ width: '100%', borderTop: '1px solid #d1d5db' }} />
              </div>
              <div style={{ position: 'relative', display: 'flex', justifyContent: 'center', fontSize: '14px' }}>
                <span style={{ padding: '0 0.5rem', background: 'white', color: '#6b7280' }}>Don't have an account?</span>
              </div>
            </div>

            <div className="text-center" style={{ marginTop: '1.5rem' }}>
              <Link
                to="/register"
                className="btn btn-ghost"
              >
                Create an account
              </Link>
            </div>
          </div>
        </div>

        {/* Back to home */}
        <div className="text-center" style={{ marginTop: '1.5rem' }}>
          <Link
            to="/"
            className="btn btn-ghost btn-small"
          >
            ← Back to home
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Login; 