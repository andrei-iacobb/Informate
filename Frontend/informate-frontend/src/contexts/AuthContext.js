import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const AuthContext = createContext();

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  // API base URL
  const API_BASE_URL = '/api';

  // Set up axios defaults
  useEffect(() => {
    const token = localStorage.getItem('authToken');
    if (token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      validateToken(token);
    } else {
      setLoading(false);
    }
  }, []);

  const validateToken = async (token) => {
    try {
      const response = await axios.get(`${API_BASE_URL}/auth/validate`);
      if (response.data.valid) {
        setCurrentUser({ username: response.data.username });
        setIsAuthenticated(true);
      } else {
        localStorage.removeItem('authToken');
        delete axios.defaults.headers.common['Authorization'];
      }
    } catch (error) {
      console.error('Token validation failed:', error);
      localStorage.removeItem('authToken');
      delete axios.defaults.headers.common['Authorization'];
    } finally {
      setLoading(false);
    }
  };

  const login = async (username, password) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/auth/login`, {
        username,
        password
      });

      if (response.data.success) {
        const token = response.data.token;
        localStorage.setItem('authToken', token);
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        setCurrentUser({ username });
        setIsAuthenticated(true);
        return { success: true };
      } else {
        return { success: false, message: response.data.message };
      }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.message || 'Login failed' 
      };
    }
  };

  const register = async (username, password) => {
    console.log('=== AUTH CONTEXT REGISTER ===');
    console.log('Username:', username);
    console.log('Password length:', password.length);
    console.log('API URL:', `${API_BASE_URL}/auth/register`);
    
    try {
      console.log('Making POST request to backend...');
      const response = await axios.post(`${API_BASE_URL}/auth/register`, {
        username,
        password
      });

      console.log('Response status:', response.status);
      console.log('Response data:', response.data);

      if (response.data.success) {
        console.log('Registration successful from backend');
        return { success: true, message: 'Registration successful' };
      } else {
        console.error('Registration failed from backend:', response.data.message);
        return { success: false, message: response.data.message };
      }
    } catch (error) {
      console.error('Registration request failed:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      return { 
        success: false, 
        message: error.response?.data?.message || 'Registration failed' 
      };
    } finally {
      console.log('=== END AUTH CONTEXT REGISTER ===');
    }
  };

  const logout = () => {
    localStorage.removeItem('authToken');
    delete axios.defaults.headers.common['Authorization'];
    setCurrentUser(null);
    setIsAuthenticated(false);
  };

  const value = {
    currentUser,
    isAuthenticated,
    loading,
    login,
    register,
    logout,
    API_BASE_URL
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
} 