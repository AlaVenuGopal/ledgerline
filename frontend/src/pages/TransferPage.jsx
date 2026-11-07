// frontend/src/pages/TransferPage.jsx
import React, { useState, useEffect } from 'react';
import { accountApi, transactionApi } from '../services/api';
import Topbar from '../components/Topbar';
import { 
    ArrowLeftRight, 
    ShieldCheck, 
    CheckCircle2, 
    AlertCircle, 
    Search,
    Key,
    RefreshCw
} from 'lucide-react';

export default function TransferPage() {
    const [accounts, setAccounts] = useState([]);
    const [sourceAccountId, setSourceAccountId] = useState('');
    const [destinationAccountNumber, setDestinationAccountNumber] = useState('');
    const [destinationInfo, setDestinationInfo] = useState(null);
    const [amountRupees, setAmountRupees] = useState('');
    const [description, setDescription] = useState('');
    const [idempotencyKey, setIdempotencyKey] = useState('');
    
    const [verifyingDest, setVerifyingDest] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [error, setError] = useState('');
    const [successReceipt, setSuccessReceipt] = useState(null);

    const generateKey = () => {
        setIdempotencyKey('IDEM-' + crypto.randomUUID());
    };

    useEffect(() => {
        generateKey();
        accountApi.getMyAccounts()
            .then(data => {
                setAccounts(data || []);
                if (data && data.length > 0) {
                    setSourceAccountId(data[0].id);
                }
            })
            .catch(err => setError(err.message || 'Failed to load accounts'));
    }, []);

    const selectedAccount = accounts.find(a => String(a.id) === String(sourceAccountId));

    const handleLookup = async () => {
        if (!destinationAccountNumber.trim()) return;
        setVerifyingDest(true);
        setError('');
        setDestinationInfo(null);
        try {
            const data = await accountApi.lookupAccount(destinationAccountNumber.trim());
            setDestinationInfo(data);
        } catch (err) {
            setError(err.message || 'Destination account not found.');
        } finally {
            setVerifyingDest(false);
        }
    };

    const handleInitiate = (e) => {
        e.preventDefault();
        setError('');
        setSuccessReceipt(null);

        const numRupees = parseFloat(amountRupees);
        if (isNaN(numRupees) || numRupees <= 0) {
            setError('Please enter a valid positive transfer amount.');
            return;
        }

        const paise = Math.round(numRupees * 100);
        if (selectedAccount && selectedAccount.balancePaise < paise) {
            setError(`Insufficient funds! Available balance is ₹${(selectedAccount.balancePaise / 100).toFixed(2)}, but you attempted to transfer ₹${numRupees.toFixed(2)}.`);
            return;
        }

        setShowConfirm(true);
    };

    const executeTransfer = async () => {
        setSubmitting(true);
        setError('');
        try {
            const paise = Math.round(parseFloat(amountRupees) * 100);
            const receipt = await transactionApi.transfer(
                Number(sourceAccountId),
                destinationAccountNumber.trim(),
                paise,
                description,
                idempotencyKey
            );

            setSuccessReceipt(receipt);
            setShowConfirm(false);
            setAmountRupees('');
            setDescription('');
            generateKey(); // Generate new idempotency key for next transaction

            // Refresh account list to update balance
            const updated = await accountApi.getMyAccounts();
            setAccounts(updated || []);
        } catch (err) {
            setError(err.message || 'Transfer failed.');
            setShowConfirm(false);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="main-content">
            <Topbar title="Internal Money Transfer" />

            <div className="page-container" style={{ maxWidth: '720px' }}>
                {error && (
                    <div className="alert alert-danger">
                        <AlertCircle size={18} style={{ flexShrink: 0 }} />
                        <span>{error}</span>
                    </div>
                )}

                {successReceipt && (
                    <div className="card" style={{ borderColor: '#10b981', backgroundColor: 'rgba(16, 185, 129, 0.05)', marginBottom: '24px' }}>
                        <div className="card-header" style={{ borderBottomColor: 'rgba(16, 185, 129, 0.2)' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#10b981' }}>
                                <CheckCircle2 size={20} />
                                <strong style={{ fontSize: '1.05rem' }}>Transfer Completed Successfully!</strong>
                            </div>
                            <span style={{ fontFamily: 'monospace', fontWeight: 700 }}>{successReceipt.transactionRef}</span>
                        </div>
                        <div className="card-body">
                            <p style={{ fontSize: '0.9rem', marginBottom: '16px' }}>
                                <strong>₹{(successReceipt.amountPaise / 100).toFixed(2)}</strong> transferred from{' '}
                                <span style={{ fontFamily: 'monospace' }}>{successReceipt.sourceAccountNumber}</span> to{' '}
                                <span style={{ fontFamily: 'monospace' }}>{successReceipt.destinationAccountNumber}</span>.
                            </p>
                            <h4 style={{ fontSize: '0.85rem', textTransform: 'uppercase', color: '#9ca3af', marginBottom: '8px' }}>
                                Double-Entry Balanced Ledger Entries:
                            </h4>
                            <div className="table-responsive">
                                <table className="table" style={{ fontSize: '0.85rem' }}>
                                    <thead>
                                        <tr>
                                            <th>Account</th>
                                            <th>Type</th>
                                            <th>Amount</th>
                                            <th>Balance After</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {successReceipt.ledgerEntries.map(e => (
                                            <tr key={e.id}>
                                                <td style={{ fontFamily: 'monospace' }}>{e.accountNumber}</td>
                                                <td>
                                                    <span className={`badge ${e.entryType === 'DEBIT' ? 'badge-debit' : 'badge-credit'}`}>
                                                        {e.entryType}
                                                    </span>
                                                </td>
                                                <td style={{ fontWeight: 700 }}>₹{(e.amountPaise / 100).toFixed(2)}</td>
                                                <td>₹{(e.balanceAfterPaise / 100).toFixed(2)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                )}

                <div className="card">
                    <div className="card-header">
                        <h2 className="card-title">Initiate Transfer</h2>
                        <span style={{ fontSize: '0.8rem', color: '#10b981', display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <ShieldCheck size={14} /> Zero-Overdraft Protected
                        </span>
                    </div>

                    <div className="card-body">
                        <form onSubmit={handleInitiate}>
                            {/* Source Account */}
                            <div className="form-group">
                                <label className="form-label">Source Account (Debit)</label>
                                <select
                                    className="form-control"
                                    value={sourceAccountId}
                                    onChange={(e) => setSourceAccountId(e.target.value)}
                                    required
                                >
                                    {accounts.map(acc => (
                                        <option key={acc.id} value={acc.id}>
                                            {acc.accountNumber} — Available: ₹{(acc.balancePaise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {/* Destination Account */}
                            <div className="form-group">
                                <label className="form-label">Destination Account Number (Credit)</label>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="e.g. LL-ACC-BOB-01"
                                        value={destinationAccountNumber}
                                        onChange={(e) => setDestinationAccountNumber(e.target.value)}
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={handleLookup}
                                        className="btn btn-secondary"
                                        disabled={verifyingDest}
                                    >
                                        <Search size={16} />
                                        <span>{verifyingDest ? 'Checking...' : 'Verify'}</span>
                                    </button>
                                </div>
                                {destinationInfo && (
                                    <div style={{ marginTop: '8px', fontSize: '0.85rem', color: '#10b981', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                        <CheckCircle2 size={16} />
                                        <span>Verified: {destinationInfo.ownerFullName} ({destinationInfo.accountNumber})</span>
                                    </div>
                                )}
                            </div>

                            {/* Amount in INR */}
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

                            {/* Description */}
                            <div className="form-group">
                                <label className="form-label">Remark / Description</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="e.g. Invoice payment or dinner split"
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                />
                            </div>

                            {/* Idempotency Key */}
                            <div className="form-group">
                                <label className="form-label" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span>Idempotency Key (Guarantees Exactly-Once Execution)</span>
                                    <button
                                        type="button"
                                        onClick={generateKey}
                                        style={{ background: 'none', border: 'none', color: '#60a5fa', cursor: 'pointer', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                                    >
                                        <RefreshCw size={12} /> Regenerate
                                    </button>
                                </label>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <Key size={16} style={{ color: '#9ca3af' }} />
                                    <input
                                        type="text"
                                        className="form-control"
                                        style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: '#9ca3af' }}
                                        value={idempotencyKey}
                                        readOnly
                                    />
                                </div>
                            </div>

                            <button
                                type="submit"
                                className="btn btn-primary"
                                style={{ width: '100%', padding: '12px', marginTop: '12px' }}
                            >
                                <ArrowLeftRight size={18} />
                                <span>Review & Transfer</span>
                            </button>
                        </form>
                    </div>
                </div>

                {/* Confirmation Modal */}
                {showConfirm && (
                    <div style={{
                        position: 'fixed',
                        top: 0, left: 0, right: 0, bottom: 0,
                        backgroundColor: 'rgba(0, 0, 0, 0.8)',
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
                            padding: '28px',
                            maxWidth: '480px',
                            width: '100%',
                            boxShadow: 'var(--shadow-lg)'
                        }}>
                            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '16px' }}>
                                Confirm Money Movement
                            </h3>
                            <div style={{ backgroundColor: 'var(--bg-canvas)', padding: '16px', borderRadius: 'var(--radius-sm)', marginBottom: '20px', fontSize: '0.9rem' }}>
                                <div style={{ marginBottom: '8px' }}>
                                    <span style={{ color: '#9ca3af' }}>From:</span> <strong>{selectedAccount?.accountNumber}</strong>
                                </div>
                                <div style={{ marginBottom: '8px' }}>
                                    <span style={{ color: '#9ca3af' }}>To:</span> <strong>{destinationAccountNumber}</strong>
                                </div>
                                <div style={{ marginBottom: '8px' }}>
                                    <span style={{ color: '#9ca3af' }}>Amount:</span> <strong style={{ color: '#10b981', fontSize: '1.1rem' }}>₹{parseFloat(amountRupees).toFixed(2)}</strong> ({Math.round(parseFloat(amountRupees) * 100)} paise)
                                </div>
                                <div>
                                    <span style={{ color: '#9ca3af' }}>Idempotency Key:</span> <span style={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>{idempotencyKey}</span>
                                </div>
                            </div>

                            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                                <button
                                    onClick={() => setShowConfirm(false)}
                                    className="btn btn-secondary"
                                    disabled={submitting}
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={executeTransfer}
                                    className="btn btn-primary"
                                    disabled={submitting}
                                >
                                    {submitting ? 'Executing Atomic Transfer...' : 'Confirm & Send'}
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
