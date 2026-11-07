// frontend/src/pages/AccountsPage.jsx
import React, { useEffect, useState } from 'react';
import { accountApi } from '../services/api';
import Topbar from '../components/Topbar';
import { Wallet, FileText, ArrowDownLeft, ArrowUpRight, X } from 'lucide-react';

export default function AccountsPage() {
    const [accounts, setAccounts] = useState([]);
    const [selectedAccount, setSelectedAccount] = useState(null);
    const [statement, setStatement] = useState(null);
    const [loading, setLoading] = useState(true);
    const [statementLoading, setStatementLoading] = useState(false);
    const [error, setError] = useState('');

    const fetchAccounts = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await accountApi.getMyAccounts();
            setAccounts(data || []);
        } catch (err) {
            setError(err.message || 'Failed to fetch accounts');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAccounts();
    }, []);

    const viewStatement = async (account) => {
        setSelectedAccount(account);
        setStatementLoading(true);
        try {
            const data = await accountApi.getStatement(account.id);
            setStatement(data);
        } catch (err) {
            setError(err.message || 'Failed to fetch statement');
        } finally {
            setStatementLoading(false);
        }
    };

    const closeStatement = () => {
        setSelectedAccount(null);
        setStatement(null);
    };

    return (
        <div className="main-content">
            <Topbar title="Accounts & Balances" />

            <div className="page-container">
                {error && <div className="alert alert-danger">{error}</div>}

                <div className="card">
                    <div className="card-header">
                        <h2 className="card-title">Registered Accounts</h2>
                        <span style={{ fontSize: '0.85rem', color: '#9ca3af' }}>{accounts.length} Total Accounts</span>
                    </div>

                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Account Number</th>
                                    <th>Owner</th>
                                    <th>Type</th>
                                    <th>Currency</th>
                                    <th>Balance (INR)</th>
                                    <th>Minor Units (Paise)</th>
                                    <th>Status</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                {accounts.length === 0 ? (
                                    <tr>
                                        <td colSpan="8" style={{ textAlign: 'center', padding: '32px', color: '#6b7280' }}>
                                            No accounts found.
                                        </td>
                                    </tr>
                                ) : (
                                    accounts.map((acc) => {
                                        const formattedBalance = (acc.balancePaise / 100).toLocaleString('en-IN', {
                                            minimumFractionDigits: 2,
                                            maximumFractionDigits: 2,
                                        });
                                        return (
                                            <tr key={acc.id}>
                                                <td style={{ fontFamily: 'monospace', fontWeight: 700, color: '#60a5fa' }}>
                                                    {acc.accountNumber}
                                                </td>
                                                <td>{acc.ownerFullName || acc.ownerUsername || 'System'}</td>
                                                <td>
                                                    <span style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
                                                        {acc.accountType}
                                                    </span>
                                                </td>
                                                <td>{acc.currency}</td>
                                                <td style={{ fontWeight: 800, fontSize: '1rem', color: '#10b981' }}>
                                                    ₹{formattedBalance}
                                                </td>
                                                <td style={{ fontFamily: 'monospace', color: '#9ca3af' }}>
                                                    {acc.balancePaise.toLocaleString()} paise
                                                </td>
                                                <td>
                                                    <span className="badge badge-active">{acc.status}</span>
                                                </td>
                                                <td>
                                                    <button
                                                        onClick={() => viewStatement(acc)}
                                                        className="btn btn-secondary"
                                                        style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                                                    >
                                                        <FileText size={14} />
                                                        <span>Statement</span>
                                                    </button>
                                                </td>
                                            </tr>
                                        );
                                    })
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                {/* Account Statement Modal / Drawer */}
                {selectedAccount && (
                    <div style={{
                        position: 'fixed',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        backgroundColor: 'rgba(0, 0, 0, 0.75)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 1000,
                        padding: '20px'
                    }}>
                        <div style={{
                            backgroundColor: 'var(--bg-surface)',
                            border: '1px solid var(--border-subtle)',
                            borderRadius: 'var(--radius-lg)',
                            width: '100%',
                            maxWidth: '850px',
                            maxHeight: '90vh',
                            display: 'flex',
                            flexDirection: 'column',
                            boxShadow: 'var(--shadow-lg)'
                        }}>
                            <div style={{
                                padding: '20px 24px',
                                borderBottom: '1px solid var(--border-subtle)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between'
                            }}>
                                <div>
                                    <h3 style={{ fontSize: '1.2rem', fontWeight: 700 }}>
                                        Account Statement — {selectedAccount.accountNumber}
                                    </h3>
                                    <p style={{ fontSize: '0.85rem', color: '#9ca3af', marginTop: '2px' }}>
                                        Owner: {selectedAccount.ownerFullName} | Current Balance: ₹{(selectedAccount.balancePaise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                                    </p>
                                </div>
                                <button onClick={closeStatement} className="btn btn-secondary" style={{ padding: '6px 10px' }}>
                                    <X size={16} />
                                </button>
                            </div>

                            <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
                                {statementLoading ? (
                                    <p style={{ textAlign: 'center', padding: '40px', color: '#9ca3af' }}>Loading ledger entries...</p>
                                ) : !statement || statement.entries.length === 0 ? (
                                    <p style={{ textAlign: 'center', padding: '40px', color: '#6b7280' }}>No statement activity recorded for this account.</p>
                                ) : (
                                    <table className="table">
                                        <thead>
                                            <tr>
                                                <th>Date & Time</th>
                                                <th>Transaction ID</th>
                                                <th>Entry Type</th>
                                                <th>Amount</th>
                                                <th>Running Balance</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {statement.entries.map((entry) => {
                                                const isCredit = entry.entryType === 'CREDIT';
                                                return (
                                                    <tr key={entry.id}>
                                                        <td style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
                                                            {new Date(entry.createdAt).toLocaleString()}
                                                        </td>
                                                        <td style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                                                            {entry.transactionId.substring(0, 8)}...
                                                        </td>
                                                        <td>
                                                            <span className={`badge ${isCredit ? 'badge-credit' : 'badge-debit'}`}>
                                                                {entry.entryType}
                                                            </span>
                                                        </td>
                                                        <td style={{ fontWeight: 700, color: isCredit ? '#10b981' : '#ef4444' }}>
                                                            {isCredit ? '+' : '-'}₹{(entry.amountPaise / 100).toFixed(2)}
                                                        </td>
                                                        <td style={{ fontWeight: 700 }}>
                                                            ₹{(entry.balanceAfterPaise / 100).toFixed(2)}
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                )}
                            </div>

                            <div style={{ padding: '16px 24px', borderTop: '1px solid var(--border-subtle)', textAlign: 'right' }}>
                                <button onClick={closeStatement} className="btn btn-secondary">Close Statement</button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
