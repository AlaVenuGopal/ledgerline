// frontend/src/pages/TransactionsPage.jsx
import React, { useEffect, useState } from 'react';
import { transactionApi } from '../services/api';
import Topbar from '../components/Topbar';
import { History, ChevronDown, ChevronUp, ShieldCheck } from 'lucide-react';

export default function TransactionsPage() {
    const [transactions, setTransactions] = useState([]);
    const [expandedId, setExpandedId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchTransactions = async () => {
        setLoading(true);
        try {
            const data = await transactionApi.getMyTransactions();
            setTransactions(data || []);
        } catch (err) {
            setError(err.message || 'Failed to fetch transactions');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTransactions();
    }, []);

    const toggleExpand = (id) => {
        setExpandedId(expandedId === id ? null : id);
    };

    return (
        <div className="main-content">
            <Topbar title="Transaction Ledger History" />

            <div className="page-container">
                {error && <div className="alert alert-danger">{error}</div>}

                <div className="card">
                    <div className="card-header">
                        <h2 className="card-title">All Financial Transactions</h2>
                        <span style={{ fontSize: '0.85rem', color: '#9ca3af' }}>{transactions.length} Total Records</span>
                    </div>

                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Reference</th>
                                    <th>Type</th>
                                    <th>Amount (INR)</th>
                                    <th>Source Account</th>
                                    <th>Destination Account</th>
                                    <th>Status</th>
                                    <th>Date</th>
                                    <th>Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                {transactions.length === 0 ? (
                                    <tr>
                                        <td colSpan="8" style={{ textAlign: 'center', padding: '32px', color: '#6b7280' }}>
                                            No transactions found.
                                        </td>
                                    </tr>
                                ) : (
                                    transactions.map((txn) => {
                                        const isExpanded = expandedId === txn.id;
                                        const amountFormatted = (txn.amountPaise / 100).toLocaleString('en-IN', {
                                            minimumFractionDigits: 2,
                                            maximumFractionDigits: 2,
                                        });

                                        return (
                                            <React.Fragment key={txn.id}>
                                                <tr>
                                                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{txn.transactionRef}</td>
                                                    <td>
                                                        <span className={`badge ${txn.type === 'DEPOSIT' ? 'badge-credit' : txn.type === 'WITHDRAWAL' ? 'badge-debit' : 'badge-credit'}`}>
                                                            {txn.type}
                                                        </span>
                                                    </td>
                                                    <td style={{ fontWeight: 700 }}>₹{amountFormatted}</td>
                                                    <td style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                                                        {txn.sourceAccountNumber || 'External'}
                                                    </td>
                                                    <td style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                                                        {txn.destinationAccountNumber || 'External'}
                                                    </td>
                                                    <td>
                                                        <span className={`badge ${txn.status === 'SUCCESS' ? 'badge-success' : 'badge-rejected'}`}>
                                                            {txn.status}
                                                        </span>
                                                    </td>
                                                    <td style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
                                                        {new Date(txn.createdAt).toLocaleString()}
                                                    </td>
                                                    <td>
                                                        <button
                                                            onClick={() => toggleExpand(txn.id)}
                                                            className="btn btn-secondary"
                                                            style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                                                        >
                                                            {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                                                            <span>Ledger</span>
                                                        </button>
                                                    </td>
                                                </tr>

                                                {/* Expanded Double-Entry Details */}
                                                {isExpanded && (
                                                    <tr style={{ backgroundColor: 'rgba(0, 0, 0, 0.25)' }}>
                                                        <td colSpan="8" style={{ padding: '16px 24px' }}>
                                                            <div style={{ marginBottom: '8px', fontSize: '0.85rem' }}>
                                                                <strong>Idempotency Key:</strong> <span style={{ fontFamily: 'monospace' }}>{txn.idempotencyKey || 'N/A'}</span>
                                                                {txn.rejectionReason && (
                                                                    <div style={{ color: '#ef4444', marginTop: '4px' }}>
                                                                        <strong>Rejection Reason:</strong> {txn.rejectionReason}
                                                                    </div>
                                                                )}
                                                            </div>

                                                            <h4 style={{ fontSize: '0.8rem', color: '#9ca3af', textTransform: 'uppercase', marginBottom: '8px' }}>
                                                                Double-Entry Balancing Entries:
                                                            </h4>

                                                            {txn.ledgerEntries && txn.ledgerEntries.length > 0 ? (
                                                                <table className="table" style={{ background: 'var(--bg-canvas)', borderRadius: '6px' }}>
                                                                    <thead>
                                                                        <tr>
                                                                            <th>Account Number</th>
                                                                            <th>Entry Type</th>
                                                                            <th>Amount (Paise)</th>
                                                                            <th>Amount (INR)</th>
                                                                            <th>Balance After</th>
                                                                        </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                        {txn.ledgerEntries.map(entry => (
                                                                            <tr key={entry.id}>
                                                                                <td style={{ fontFamily: 'monospace' }}>{entry.accountNumber}</td>
                                                                                <td>
                                                                                    <span className={`badge ${entry.entryType === 'DEBIT' ? 'badge-debit' : 'badge-credit'}`}>
                                                                                        {entry.entryType}
                                                                                    </span>
                                                                                </td>
                                                                                <td style={{ fontFamily: 'monospace' }}>{entry.amountPaise} paise</td>
                                                                                <td style={{ fontWeight: 700 }}>₹{(entry.amountPaise / 100).toFixed(2)}</td>
                                                                                <td>₹{(entry.balanceAfterPaise / 100).toFixed(2)}</td>
                                                                            </tr>
                                                                        ))}
                                                                    </tbody>
                                                                </table>
                                                            ) : (
                                                                <div style={{ fontSize: '0.85rem', color: '#6b7280' }}>
                                                                    No financial entries created (transaction rejected before ledger modification).
                                                                </div>
                                                            )}
                                                        </td>
                                                    </tr>
                                                )}
                                            </React.Fragment>
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
