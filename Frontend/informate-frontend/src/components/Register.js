import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Register = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError(''); // Clear error when user types
    setSuccess(''); // Clear success when user types
  };

  const validateForm = () => {
    if (formData.username.length < 3) {
      setError('Username must be at least 3 characters long');
      return false;
    }
    
    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters long');
      return false;
    }
    
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return false;
    }
    
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    console.log('=== REGISTRATION FORM SUBMIT ===');
    console.log('Form data:', formData);
    
    if (!validateForm()) {
      console.log('Form validation failed');
      return;
    }
    
    setLoading(true);
    setError('');

    try {
      console.log('Calling register function...');
      const result = await register(formData.username, formData.password);
      console.log('Register result:', result);
      
      if (result.success) {
        console.log('Registration successful!');
        setSuccess('Registration successful! Redirecting to login...');
        setTimeout(() => {
          navigate('/login');
        }, 2000);
      } else {
        console.error('Registration failed:', result.message);
        setError(result.message);
      }
    } catch (err) {
      console.error('Registration error:', err);
      setError('An unexpected error occurred');
    } finally {
      setLoading(false);
      console.log('=== END REGISTRATION FORM SUBMIT ===');
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
          <h2 className="hero-title" style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>Create your account</h2>
          <p className="hero-subtitle" style={{ fontSize: '14px', marginBottom: '2rem' }}>Join thousands of users staying informed with AI</p>
        </div>

        {/* Registration Form */}
        <div>
          {error && (
            <div className="alert alert-error">
              {error}
            </div>
          )}
          
          {success && (
            <div className="alert alert-success">
              {success}
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
                placeholder="Choose a username"
              />
              <p className="form-help">At least 3 characters</p>
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
                placeholder="Create a password"
              />
              <p className="form-help">At least 6 characters</p>
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword" className="form-label">
                Confirm Password
              </label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                required
                value={formData.confirmPassword}
                onChange={handleChange}
                className="form-input"
                placeholder="Confirm your password"
              />
            </div>

            <div className="form-group">
              <button
                type="submit"
                disabled={loading || success}
                className="btn btn-primary"
                style={{ width: '100%' }}
              >
                {loading ? (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <div className="spinner spinner-small" style={{ marginRight: '0.5rem' }}></div>
                    Creating account...
                  </div>
                ) : success ? (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <svg style={{ width: '20px', height: '20px', marginRight: '0.5rem' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                    Account created!
                  </div>
                ) : (
                  'Create account'
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
                <span style={{ padding: '0 0.5rem', background: 'white', color: '#6b7280' }}>Already have an account?</span>
              </div>
            </div>

            <div className="text-center" style={{ marginTop: '1.5rem' }}>
              <Link
                to="/login"
                className="btn btn-ghost"
              >
                Sign in here
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

export default Register; 