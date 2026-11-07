// frontend/src/context/AuthContext.jsx
import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const storedUser = localStorage.getItem('ledgerline_user');
        const token = localStorage.getItem('ledgerline_token');
        if (storedUser && token) {
            try {
                setUser(JSON.parse(storedUser));
            } catch (e) {
                localStorage.removeItem('ledgerline_user');
                localStorage.removeItem('ledgerline_token');
            }
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
        const response = await authApi.login(username, password);
        const userData = {
            id: response.id,
            username: response.username,
            email: response.email,
            fullName: response.fullName,
            role: response.role,
        };
        localStorage.setItem('ledgerline_token', response.token);
        localStorage.setItem('ledgerline_user', JSON.stringify(userData));
        setUser(userData);
        return userData;
    };

    const logout = () => {
        localStorage.removeItem('ledgerline_token');
        localStorage.removeItem('ledgerline_user');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading, isAuthenticated: !!user }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
