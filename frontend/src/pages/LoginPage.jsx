// frontend/src/pages/LoginPage.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, ArrowRight, Lock, User, AlertCircle } from 'lucide-react';

export default function LoginPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        if (e) e.preventDefault();
        setError('');
        setLoading(true);

        try {
            await login(username, password);
            navigate('/');
        } catch (err) {
            setError(err.message || 'Login failed. Please check your credentials.');
        } finally {
            setLoading(false);
        }
    };

    const handleQuickLogin = (u, p) => {
        setUsername(u);
        setPassword(p);
        setError('');
        setLoading(true);
        login(u, p)
            .then(() => navigate('/'))
            .catch((err) => setError(err.message || 'Login failed.'))
            .finally(() => setLoading(false));
    };

    return (
        <div style={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'var(--bg-canvas)',
            padding: '20px'
        }}>
            <div style={{
                width: '100%',
                maxWidth: '440px',
                backgroundColor: 'var(--bg-surface)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-lg)',
                padding: '36px',
                boxShadow: 'var(--shadow-lg)'
            }}>
                <div style={{ textAlign: 'center', marginBottom: '28px' }}>
                    <div style={{
                        display: 'inline-flex',
                        padding: '12px',
                        borderRadius: 'var(--radius-md)',
                        backgroundColor: 'var(--primary-light)',
                        color: 'var(--primary)',
                        marginBottom: '16px'
                    }}>
                        <ShieldCheck size={32} />
                    </div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-primary)' }}>LedgerLine</h1>
                    <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
                        Money Movement with a Ledger You Can Prove
                    </p>
                </div>

                {error && (
                    <div className="alert alert-danger">
                        <AlertCircle size={18} style={{ flexShrink: 0 }} />
                        <span>{error}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label" htmlFor="username">Username</label>
                        <div style={{ position: 'relative' }}>
                            <User size={18} style={{ position: 'absolute', left: '12px', top: '13px', color: '#6b7280' }} />
                            <input
                                id="username"
                                type="text"
                                className="form-control"
                                style={{ paddingLeft: '40px' }}
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="Enter username"
                                required
                            />
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="password">Password</label>
                        <div style={{ position: 'relative' }}>
                            <Lock size={18} style={{ position: 'absolute', left: '12px', top: '13px', color: '#6b7280' }} />
                            <input
                                id="password"
                                type="password"
                                className="form-control"
                                style={{ paddingLeft: '40px' }}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Enter password"
                                required
                            />
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary"
                        style={{ width: '100%', marginTop: '8px', padding: '12px' }}
                        disabled={loading}
                    >
                        {loading ? 'Authenticating...' : 'Sign In to Ledger'}
                        {!loading && <ArrowRight size={16} />}
                    </button>
                </form>

                <div style={{ marginTop: '28px', borderTop: '1px solid var(--border-subtle)', paddingTop: '20px' }}>
                    <p style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '12px', textAlign: 'center' }}>
                        Quick Demo Sign-In (Snovasys Evaluation)
                    </p>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            style={{ fontSize: '0.8rem', padding: '8px 10px' }}
                            onClick={() => handleQuickLogin('alice', 'AlicePassword@123')}
                        >
                            Alice (Account Holder)
                        </button>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            style={{ fontSize: '0.8rem', padding: '8px 10px' }}
                            onClick={() => handleQuickLogin('bob', 'BobPassword@123')}
                        >
                            Bob (Account Holder)
                        </button>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            style={{ fontSize: '0.8rem', padding: '8px 10px' }}
                            onClick={() => handleQuickLogin('admin', 'AdminPassword@123')}
                        >
                            Admin (Administrator)
                        </button>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            style={{ fontSize: '0.8rem', padding: '8px 10px' }}
                            onClick={() => handleQuickLogin('auditor', 'AuditorPassword@123')}
                        >
                            Auditor (Compliance)
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
