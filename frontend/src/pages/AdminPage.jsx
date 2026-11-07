// frontend/src/pages/AdminPage.jsx
import React, { useEffect, useState } from 'react';
import { adminApi } from '../services/api';
import Topbar from '../components/Topbar';
import { Users, UserPlus, CreditCard, CheckCircle2, AlertCircle } from 'lucide-react';

export default function AdminPage() {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    // Create User Form State
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [fullName, setFullName] = useState('');
    const [role, setRole] = useState('ROLE_ACCOUNT_HOLDER');

    // Create Account Form State
    const [selectedUserId, setSelectedUserId] = useState('');

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const data = await adminApi.getUsers();
            setUsers(data || []);
            if (data && data.length > 0 && !selectedUserId) {
                setSelectedUserId(data[0].id);
            }
        } catch (err) {
            setError(err.message || 'Failed to fetch users');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const handleCreateUser = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');
        try {
            const created = await adminApi.createUser({ username, email, password, fullName, role });
            setSuccessMessage(`User '${created.username}' created successfully!`);
            setUsername('');
            setEmail('');
            setPassword('');
            setFullName('');
            fetchUsers();
        } catch (err) {
            setError(err.message || 'Failed to create user');
        }
    };

    const handleCreateAccount = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');
        try {
            const account = await adminApi.createAccount({ userId: Number(selectedUserId), currency: 'INR' });
            setSuccessMessage(`Account '${account.accountNumber}' created successfully for user ID ${selectedUserId}!`);
        } catch (err) {
            setError(err.message || 'Failed to create account');
        }
    };

    return (
        <div className="main-content">
            <Topbar title="Administrator Control Panel" />

            <div className="page-container">
                {error && <div className="alert alert-danger">{error}</div>}
                {successMessage && (
                    <div className="alert alert-success">
                        <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
                        <span>{successMessage}</span>
                    </div>
                )}

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '24px', marginBottom: '32px' }}>
                    {/* Create User Card */}
                    <div className="card">
                        <div className="card-header">
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <UserPlus size={18} style={{ color: '#10b981' }} />
                                <h2 className="card-title">Create User</h2>
                            </div>
                        </div>
                        <div className="card-body">
                            <form onSubmit={handleCreateUser}>
                                <div className="form-group">
                                    <label className="form-label">Username</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="e.g. charlie"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Full Name</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="e.g. Charlie Rao"
                                        value={fullName}
                                        onChange={(e) => setFullName(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Email</label>
                                    <input
                                        type="email"
                                        className="form-control"
                                        placeholder="charlie@example.com"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Password</label>
                                    <input
                                        type="password"
                                        className="form-control"
                                        placeholder="Min 8 characters"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Role</label>
                                    <select
                                        className="form-control"
                                        value={role}
                                        onChange={(e) => setRole(e.target.value)}
                                    >
                                        <option value="ROLE_ACCOUNT_HOLDER">Account Holder</option>
                                        <option value="ROLE_ADMINISTRATOR">Administrator</option>
                                        <option value="ROLE_AUDITOR">Auditor</option>
                                    </select>
                                </div>
                                <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
                                    Create User
                                </button>
                            </form>
                        </div>
                    </div>

                    {/* Create Account Card */}
                    <div className="card">
                        <div className="card-header">
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <CreditCard size={18} style={{ color: '#60a5fa' }} />
                                <h2 className="card-title">Open New Account</h2>
                            </div>
                        </div>
                        <div className="card-body">
                            <form onSubmit={handleCreateAccount}>
                                <div className="form-group">
                                    <label className="form-label">Account Owner</label>
                                    <select
                                        className="form-control"
                                        value={selectedUserId}
                                        onChange={(e) => setSelectedUserId(e.target.value)}
                                        required
                                    >
                                        {users.map(u => (
                                            <option key={u.id} value={u.id}>
                                                {u.fullName} (@{u.username}) — {u.role}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Currency</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value="INR (Indian Rupee)"
                                        readOnly
                                    />
                                </div>
                                <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '36px' }}>
                                    Open Customer Account
                                </button>
                            </form>
                        </div>
                    </div>
                </div>

                {/* System Users Table */}
                <div className="card">
                    <div className="card-header">
                        <h2 className="card-title">Registered System Users</h2>
                        <span style={{ fontSize: '0.85rem', color: '#9ca3af' }}>{users.length} Users</span>
                    </div>
                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>User ID</th>
                                    <th>Username</th>
                                    <th>Full Name</th>
                                    <th>Email</th>
                                    <th>Role</th>
                                    <th>Status</th>
                                    <th>Registered</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map(u => (
                                    <tr key={u.id}>
                                        <td style={{ fontFamily: 'monospace' }}>#{u.id}</td>
                                        <td style={{ fontWeight: 700 }}>{u.username}</td>
                                        <td>{u.fullName}</td>
                                        <td style={{ color: '#9ca3af' }}>{u.email}</td>
                                        <td>
                                            <span className={`user-role-badge ${u.role === 'ROLE_ADMINISTRATOR' ? 'role-admin' : u.role === 'ROLE_AUDITOR' ? 'role-auditor' : 'role-holder'}`}>
                                                {u.role.replace('ROLE_', '')}
                                            </span>
                                        </td>
                                        <td>
                                            <span className="badge badge-active">{u.status}</span>
                                        </td>
                                        <td style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
                                            {new Date(u.createdAt).toLocaleDateString()}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
}
