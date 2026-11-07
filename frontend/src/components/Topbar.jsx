// frontend/src/components/Topbar.jsx
import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Shield, CheckCircle } from 'lucide-react';

export default function Topbar({ title }) {
    const { user } = useAuth();

    return (
        <header className="top-bar">
            <h1 className="page-title">{title}</h1>
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    gap: '6px', 
                    fontSize: '0.8rem', 
                    color: '#10b981', 
                    backgroundColor: 'rgba(16, 185, 129, 0.1)', 
                    padding: '4px 10px', 
                    borderRadius: '20px' 
                }}>
                    <CheckCircle size={14} />
                    <span>Double-Entry Protected</span>
                </div>
                <div style={{ fontSize: '0.85rem', color: '#9ca3af' }}>
                    Signed in as <strong style={{ color: '#f9fafb' }}>{user?.username}</strong>
                </div>
            </div>
        </header>
    );
}
