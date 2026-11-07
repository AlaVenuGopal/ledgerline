// frontend/src/pages/AuditorPage.jsx
import React, { useEffect, useState } from 'react';
import { auditorApi } from '../services/api';
import Topbar from '../components/Topbar';
import { ShieldCheck, CheckCircle2, AlertCircle, RefreshCw, Lock } from 'lucide-react';

export default function AuditorPage() {
    const [integrity, setIntegrity] = useState(null);
    const [auditEvents, setAuditEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [verifying, setVerifying] = useState(false);
    const [error, setError] = useState('');

    const fetchData = async () => {
        setLoading(true);
        setError('');
        try {
            const [integ, events] = await Promise.all([
                auditorApi.getLedgerIntegrity(),
                auditorApi.getAuditEvents(),
            ]);
            setIntegrity(integ);
            setAuditEvents(events || []);
        } catch (err) {
            setError(err.message || 'Failed to load audit records');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const runLiveProof = async () => {
        setVerifying(true);
        try {
            const data = await auditorApi.getLedgerIntegrity();
            setIntegrity(data);
        } catch (err) {
            setError(err.message || 'Integrity check failed');
        } finally {
            setVerifying(false);
        }
    };

    return (
        <div className="main-content">
            <Topbar title="Auditor Portal & Mathematical Ledger Proof" />

            <div className="page-container">
                {error && <div className="alert alert-danger">{error}</div>}

                {/* Mathematical Proof Card */}
                <div className="card" style={{ border: '2px solid #10b981', backgroundColor: 'rgba(16, 185, 129, 0.04)' }}>
                    <div className="card-header" style={{ borderBottomColor: 'rgba(16, 185, 129, 0.2)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <ShieldCheck size={24} style={{ color: '#10b981' }} />
                            <div>
                                <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#f9fafb' }}>
                                    Mathematical Double-Entry Conservation Proof
                                </h2>
                                <p style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
                                    Provable invariant: SUM(Debits) == SUM(Credits) globally across all customer and system accounts.
                                </p>
                            </div>
                        </div>

                        <button 
                            onClick={runLiveProof} 
                            className="btn btn-primary"
                            disabled={verifying}
                            style={{ padding: '8px 16px' }}
                        >
                            <RefreshCw size={14} className={verifying ? 'spin' : ''} />
                            <span>{verifying ? 'Proving Invariant...' : 'Run Live Proof'}</span>
                        </button>
                    </div>

                    <div className="card-body">
                        {integrity && (
                            <div className="stats-grid" style={{ marginBottom: 0 }}>
                                <div className="stat-card">
                                    <span className="stat-label">Total Debits</span>
                                    <div className="stat-value" style={{ color: '#ef4444' }}>
                                        ₹{(integrity.totalDebitsPaise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                                    </div>
                                    <span className="stat-subtitle">{integrity.totalDebitsPaise.toLocaleString()} paise</span>
                                </div>

                                <div className="stat-card">
                                    <span className="stat-label">Total Credits</span>
                                    <div className="stat-value" style={{ color: '#10b981' }}>
                                        ₹{(integrity.totalCreditsPaise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                                    </div>
                                    <span className="stat-subtitle">{integrity.totalCreditsPaise.toLocaleString()} paise</span>
                                </div>

                                <div className="stat-card">
                                    <span className="stat-label">Net Discrepancy</span>
                                    <div className="stat-value" style={{ color: integrity.differencePaise === 0 ? '#10b981' : '#ef4444' }}>
                                        ₹{(integrity.differencePaise / 100).toFixed(2)}
                                    </div>
                                    <span className="stat-subtitle">
                                        {integrity.isBalanced ? '✓ 0 Paise Imbalance — Invariant Preserved' : 'IMBALANCE DETECTED'}
                                    </span>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* Append-Only Audit Trail Table */}
                <div className="card">
                    <div className="card-header">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <Lock size={18} style={{ color: '#60a5fa' }} />
                            <h2 className="card-title">Append-Only Audit Trail (Immutable Events)</h2>
                        </div>
                        <span style={{ fontSize: '0.85rem', color: '#9ca3af' }}>{auditEvents.length} Events Logged</span>
                    </div>

                    <div className="table-responsive">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>Event Type</th>
                                    <th>Actor</th>
                                    <th>Target Account</th>
                                    <th>Status</th>
                                    <th>IP Address</th>
                                    <th>Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                {auditEvents.length === 0 ? (
                                    <tr>
                                        <td colSpan="7" style={{ textAlign: 'center', padding: '32px', color: '#6b7280' }}>
                                            No audit events recorded yet.
                                        </td>
                                    </tr>
                                ) : (
                                    auditEvents.map(evt => (
                                        <tr key={evt.id}>
                                            <td style={{ fontSize: '0.8rem', color: '#9ca3af' }}>
                                                {new Date(evt.createdAt).toLocaleString()}
                                            </td>
                                            <td>
                                                <span style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '0.8rem', color: '#60a5fa' }}>
                                                    {evt.eventType}
                                                </span>
                                            </td>
                                            <td style={{ fontWeight: 600 }}>{evt.actorUsername}</td>
                                            <td style={{ fontFamily: 'monospace' }}>
                                                {evt.targetAccountId ? `Acc #${evt.targetAccountId}` : '—'}
                                            </td>
                                            <td>
                                                <span className={`badge ${evt.status === 'SUCCESS' ? 'badge-success' : 'badge-rejected'}`}>
                                                    {evt.status}
                                                </span>
                                            </td>
                                            <td style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: '#9ca3af' }}>
                                                {evt.ipAddress || '127.0.0.1'}
                                            </td>
                                            <td style={{ fontSize: '0.85rem', color: '#e5e7eb' }}>
                                                {evt.details}
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
}
