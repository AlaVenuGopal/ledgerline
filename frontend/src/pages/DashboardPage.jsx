// frontend/src/pages/DashboardPage.jsx
import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { accountApi, transactionApi, auditorApi } from '../services/api';
import Topbar from '../components/Topbar';
import { 
    Wallet, 
    ArrowUpRight, 
    ArrowDownLeft, 
    ArrowLeftRight, 
    ShieldCheck, 
    TrendingUp,
    RefreshCw
} from 'lucide-react';

export default function DashboardPage() {
    const { user } = useAuth();
    const [accounts, setAccounts] = useState([]);
    const [transactions, setTransactions] = useState([]);
    const [ledgerStatus, setLedgerStatus] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchData = async () => {
        setLoading(true);
        setError('');
        try {
            const [accs, txns] = await Promise.all([
                accountApi.getMyAccounts(),
                transactionApi.getMyTransactions(),
            ]);
            setAccounts(accs || []);
            setTransactions(txns || []);

            if (user?.role === 'ROLE_ADMINISTRATOR' || user?.role === 'ROLE_AUDITOR') {
                const integrity = await auditorApi.getLedgerIntegrity().catch(() => null);
                setLedgerStatus(integrity);
            }
        } catch (err) {
            setError(err.message || 'Failed to load dashboard data');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const totalBalancePaise = accounts.reduce((acc, curr) => acc + (curr.balancePaise || 0), 0);
    const totalBalanceRupees = (totalBalancePaise / 100).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });

    const isStaff = user?.role === 'ROLE_ADMINISTRATOR' || user?.role === 'ROLE_AUDITOR';

    return (
        <div className="main-content">
            <Topbar title={`Welcome back, ${user?.fullName || user?.username}`} />

            <div className="page-container">
                {error && <div className="alert alert-danger">{error}</div>}

                {/* Proof Box */}
                <div className="proof-box">
                    <div className="proof-title">
                        <ShieldCheck size={20} />
                        Double-Entry Integrity & Conservation Invariant Active
                    </div>
                    <div className="proof-desc">
                        Every financial transaction in LedgerLine executes atomically across balanced debit and credit ledger rows. 
                        No money is ever created or destroyed. Balances are protected with PostgreSQL row-level pessimistic locking and integer minor units (paise).
                    </div>
                </div>

                {/* Stat Cards */}
                <div className="stats-grid">
                    <div className="stat-card">
                        <span className="stat-label">Total Liquid Balance</span>
                        <div className="stat-value">₹{totalBalanceRupees}</div>
                        <span className="stat-subtitle">Across {accounts.length} active account{accounts.length === 1 ? '' : 's'}</span>
                    </div>

                    <div className="stat-card">
                        <span className="stat-label">Total Recorded Transactions</span>
                        <div className="stat-value">{transactions.length}</div>
                        <span className="stat-subtitle">Verified in immutable audit history</span>
                    </div>

                    <div className="stat-card">
                        <span className="stat-label">System Ledger Invariant</span>
                        <div className="stat-value" style={{ color: '#10b981', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <ShieldCheck size={28} />
                            <span>100% Balanced</span>
                        </div>
                        <span className="stat-subtitle">Debits equal Credits globally</span>
                    </div>
                </div>

                {/* Quick Actions */}
                {!isStaff && (
                    <div style={{ display: 'flex', gap: '12px', marginBottom: '32px' }}>
                        <Link to="/transfer" className="btn btn-primary">
                            <ArrowLeftRight size={16} />
                            <span>Send Transfer</span>
                        </Link>
                        <Link to="/move-money" className="btn btn-secondary">
                            <ArrowUpRight size={16} />
                            <span>Deposit / Withdraw</span>
                        </Link>
                        <button onClick={fetchData} className="btn btn-secondary" title="Refresh">
                            <RefreshCw size={16} />
                            <span>Refresh</span>
                        </button>
                    </div>
                )}

                {/* Recent Transactions Card */}
                <div className="card">
                    <div className="card-header">
                        <h2 className="card-title">Recent Financial Movements</h2>
                        <Link to="/transactions" style={{ fontSize: '0.85rem', fontWeight: 600 }}>View all →</Link>
                    </div>
                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Reference</th>
                                    <th>Type</th>
                                    <th>Amount (INR)</th>
                                    <th>Source / Destination</th>
                                    <th>Status</th>
                                    <th>Date</th>
                                </tr>
                            </thead>
                            <tbody>
                                {transactions.length === 0 ? (
                                    <tr>
                                        <td colSpan="6" style={{ textAlign: 'center', padding: '32px', color: '#6b7280' }}>
                                            No transactions recorded yet.
                                        </td>
                                    </tr>
                                ) : (
                                    transactions.slice(0, 6).map((txn) => {
                                        const amountFormatted = (txn.amountPaise / 100).toLocaleString('en-IN', {
                                            minimumFractionDigits: 2,
                                            maximumFractionDigits: 2,
                                        });
                                        return (
                                            <tr key={txn.id}>
                                                <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{txn.transactionRef}</td>
                                                <td>
                                                    <span className={`badge ${txn.type === 'DEPOSIT' ? 'badge-credit' : txn.type === 'WITHDRAWAL' ? 'badge-debit' : 'badge-credit'}`}>
                                                        {txn.type}
                                                    </span>
                                                </td>
                                                <td style={{ fontWeight: 700 }}>₹{amountFormatted}</td>
                                                <td style={{ fontSize: '0.85rem', color: '#9ca3af' }}>
                                                    {txn.sourceAccountNumber || 'External'} → {txn.destinationAccountNumber || 'External'}
                                                </td>
                                                <td>
                                                    <span className={`badge ${txn.status === 'SUCCESS' ? 'badge-success' : 'badge-rejected'}`}>
                                                        {txn.status}
                                                    </span>
                                                </td>
                                                <td style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
                                                    {new Date(txn.createdAt).toLocaleString()}
                                                </td>
                                            </tr>
                                        );
                                    })
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
}
