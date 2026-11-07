// frontend/src/pages/DepositWithdrawPage.jsx
import React, { useState, useEffect } from 'react';
import { accountApi, transactionApi } from '../services/api';
import Topbar from '../components/Topbar';
import { ArrowDownLeft, ArrowUpRight, CheckCircle2, AlertCircle } from 'lucide-react';

export default function DepositWithdrawPage() {
    const [accounts, setAccounts] = useState([]);
    const [selectedAccountId, setSelectedAccountId] = useState('');
    const [activeTab, setActiveTab] = useState('DEPOSIT'); // DEPOSIT or WITHDRAW
    const [amountRupees, setAmountRupees] = useState('');
    const [description, setDescription] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [receipt, setReceipt] = useState(null);

    const fetchAccounts = async () => {
        try {
            const data = await accountApi.getMyAccounts();
            setAccounts(data || []);
            if (data && data.length > 0 && !selectedAccountId) {
                setSelectedAccountId(data[0].id);
            }
        } catch (err) {
            setError(err.message || 'Failed to load accounts');
        }
    };

    useEffect(() => {
        fetchAccounts();
    }, []);

    const selectedAccount = accounts.find(a => String(a.id) === String(selectedAccountId));

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setReceipt(null);

        const rupees = parseFloat(amountRupees);
        if (isNaN(rupees) || rupees <= 0) {
            setError('Please enter a valid positive amount.');
            return;
        }

        const paise = Math.round(rupees * 100);
        const idempotencyKey = 'IDEM-' + crypto.randomUUID();

        setLoading(true);
        try {
            let res;
            if (activeTab === 'DEPOSIT') {
                res = await transactionApi.deposit(Number(selectedAccountId), paise, description, idempotencyKey);
            } else {
                res = await transactionApi.withdraw(Number(selectedAccountId), paise, description, idempotencyKey);
            }
            setReceipt(res);
            setAmountRupees('');
            setDescription('');
            fetchAccounts(); // Update balance
        } catch (err) {
            setError(err.message || 'Transaction failed.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="main-content">
            <Topbar title="Deposit & Withdraw" />

            <div className="page-container" style={{ maxWidth: '640px' }}>
                {error && (
                    <div className="alert alert-danger">
                        <AlertCircle size={18} style={{ flexShrink: 0 }} />
                        <span>{error}</span>
                    </div>
                )}

                {receipt && (
                    <div className="alert alert-success">
                        <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
                        <div>
                            <strong>{receipt.type} Successful!</strong> Transaction Ref: {receipt.transactionRef} (Amount: ₹{(receipt.amountPaise / 100).toFixed(2)})
                        </div>
                    </div>
                )}

                <div className="card">
                    <div style={{ display: 'flex', borderBottom: '1px solid var(--border-subtle)' }}>
                        <button
                            type="button"
                            onClick={() => { setActiveTab('DEPOSIT'); setReceipt(null); setError(''); }}
                            style={{
                                flex: 1,
                                padding: '16px',
                                background: activeTab === 'DEPOSIT' ? 'var(--bg-surface)' : 'var(--bg-surface-elevated)',
                                border: 'none',
                                borderBottom: activeTab === 'DEPOSIT' ? '2px solid #10b981' : 'none',
                                color: activeTab === 'DEPOSIT' ? '#10b981' : '#9ca3af',
                                fontWeight: 700,
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '8px'
                            }}
                        >
                            <ArrowDownLeft size={18} />
                            <span>Deposit Funds</span>
                        </button>
                        <button
                            type="button"
                            onClick={() => { setActiveTab('WITHDRAW'); setReceipt(null); setError(''); }}
                            style={{
                                flex: 1,
                                padding: '16px',
                                background: activeTab === 'WITHDRAW' ? 'var(--bg-surface)' : 'var(--bg-surface-elevated)',
                                border: 'none',
                                borderBottom: activeTab === 'WITHDRAW' ? '2px solid #ef4444' : 'none',
                                color: activeTab === 'WITHDRAW' ? '#ef4444' : '#9ca3af',
                                fontWeight: 700,
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '8px'
                            }}
                        >
                            <ArrowUpRight size={18} />
                            <span>Withdraw Funds</span>
                        </button>
                    </div>

                    <div className="card-body">
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label className="form-label">Select Account</label>
                                <select
                                    className="form-control"
                                    value={selectedAccountId}
                                    onChange={(e) => setSelectedAccountId(e.target.value)}
                                    required
                                >
                                    {accounts.map(acc => (
                                        <option key={acc.id} value={acc.id}>
                                            {acc.accountNumber} — Available: ₹{(acc.balancePaise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Amount (₹ INR)</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    min="0.01"
                                    className="form-control"
                                    placeholder="0.00"
                                    value={amountRupees}
                                    onChange={(e) => setAmountRupees(e.target.value)}
                                    required
                                />
                                {amountRupees && !isNaN(parseFloat(amountRupees)) && (
                                    <div className="form-hint">
                                        Equivalent minor units: <strong>{Math.round(parseFloat(amountRupees) * 100)} paise</strong>
                                    </div>
                                )}
                            </div>

                            <div className="form-group">
                                <label className="form-label">Description</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder={activeTab === 'DEPOSIT' ? 'e.g. Salary or Cash Deposit' : 'e.g. ATM or Cash Withdrawal'}
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                />
                            </div>

                            <button
                                type="submit"
                                className={`btn ${activeTab === 'DEPOSIT' ? 'btn-primary' : 'btn-danger'}`}
                                style={{ width: '100%', padding: '12px' }}
                                disabled={loading}
                            >
                                {loading ? 'Processing Atomic Movement...' : activeTab === 'DEPOSIT' ? 'Complete Deposit' : 'Complete Withdrawal'}
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    );
}
