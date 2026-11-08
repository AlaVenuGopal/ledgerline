// frontend/src/services/api.js
// Centralized API Service Layer for LedgerLine

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088';

export async function apiRequest(endpoint, options = {}) {
    const token = localStorage.getItem('ledgerline_token');

    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...options.headers,
    };

    const config = {
        ...options,
        headers,
    };

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

        if (response.status === 401 && !endpoint.includes('/api/auth/login')) {
            localStorage.removeItem('ledgerline_token');
            localStorage.removeItem('ledgerline_user');
            window.location.href = '/login';
            throw new Error('Session expired. Please log in again.');
        }

        const data = await response.json().catch(() => null);

        if (!response.ok) {
            const errorMessage = data?.message || data?.error || `Request failed with status ${response.status}`;
            const error = new Error(errorMessage);
            error.status = response.status;
            error.details = data?.details;
            throw error;
        }

        return data;
    } catch (err) {
        if (!err.status) {
            // Network or CORS error
            throw new Error(err.message || 'Network error. Please ensure backend is running.');
        }
        throw err;
    }
}

// 1. Auth APIs
export const authApi = {
    login: (username, password) => apiRequest('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
    }),
};

// 2. Account APIs
export const accountApi = {
    getMyAccounts: () => apiRequest('/api/accounts'),
    getAccountById: (id) => apiRequest(`/api/accounts/${id}`),
    lookupAccount: (accountNumber) => apiRequest(`/api/accounts/lookup/${accountNumber}`),
    getStatement: (id) => apiRequest(`/api/accounts/${id}/statement`),
};

// 3. Financial Transaction APIs
export const transactionApi = {
    getMyTransactions: () => apiRequest('/api/transactions'),
    deposit: (accountId, amountPaise, description, idempotencyKey) => apiRequest('/api/transactions/deposit', {
        method: 'POST',
        headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {},
        body: JSON.stringify({ accountId, amountPaise, description, idempotencyKey }),
    }),
    withdraw: (accountId, amountPaise, description, idempotencyKey) => apiRequest('/api/transactions/withdraw', {
        method: 'POST',
        headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {},
        body: JSON.stringify({ accountId, amountPaise, description, idempotencyKey }),
    }),
    transfer: (sourceAccountId, destinationAccountNumber, amountPaise, description, idempotencyKey) => apiRequest('/api/transactions/transfer', {
        method: 'POST',
        headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {},
        body: JSON.stringify({ sourceAccountId, destinationAccountNumber, amountPaise, description, idempotencyKey }),
    }),
};

// 4. Admin APIs
export const adminApi = {
    getUsers: () => apiRequest('/api/admin/users'),
    createUser: (userData) => apiRequest('/api/admin/users', {
        method: 'POST',
        body: JSON.stringify(userData),
    }),
    createAccount: (accountData) => apiRequest('/api/admin/accounts', {
        method: 'POST',
        body: JSON.stringify(accountData),
    }),
};

// 5. Auditor APIs
export const auditorApi = {
    getAuditEvents: () => apiRequest('/api/auditor/audit-events'),
    getLedgerEntries: () => apiRequest('/api/auditor/ledger-entries'),
    getLedgerIntegrity: () => apiRequest('/api/auditor/ledger-integrity'),
};

// 6. Health Check API
export const healthApi = {
    check: () => apiRequest('/api/health'),
};
