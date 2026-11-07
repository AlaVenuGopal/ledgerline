// frontend/src/App.jsx
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';

import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import AccountsPage from './pages/AccountsPage';
import TransferPage from './pages/TransferPage';
import DepositWithdrawPage from './pages/DepositWithdrawPage';
import TransactionsPage from './pages/TransactionsPage';
import AuditorPage from './pages/AuditorPage';
import AdminPage from './pages/AdminPage';

function ProtectedLayout({ children, allowedRoles }) {
    const { user, loading, isAuthenticated } = useAuth();

    if (loading) {
        return (
            <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'var(--bg-canvas)' }}>
                <p style={{ color: 'var(--text-secondary)' }}>Verifying session...</p>
            </div>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (allowedRoles && !allowedRoles.includes(user.role)) {
        return <Navigate to="/" replace />;
    }

    return (
        <div className="app-layout">
            <Sidebar />
            {children}
        </div>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />

                    <Route path="/" element={
                        <ProtectedLayout>
                            <DashboardPage />
                        </ProtectedLayout>
                    } />

                    <Route path="/accounts" element={
                        <ProtectedLayout>
                            <AccountsPage />
                        </ProtectedLayout>
                    } />

                    <Route path="/transfer" element={
                        <ProtectedLayout allowedRoles={['ROLE_ACCOUNT_HOLDER', 'ROLE_ADMINISTRATOR']}>
                            <TransferPage />
                        </ProtectedLayout>
                    } />

                    <Route path="/move-money" element={
                        <ProtectedLayout allowedRoles={['ROLE_ACCOUNT_HOLDER', 'ROLE_ADMINISTRATOR']}>
                            <DepositWithdrawPage />
                        </ProtectedLayout>
                    } />

                    <Route path="/transactions" element={
                        <ProtectedLayout>
                            <TransactionsPage />
                        </ProtectedLayout>
                    } />

                    <Route path="/auditor" element={
                        <ProtectedLayout allowedRoles={['ROLE_AUDITOR', 'ROLE_ADMINISTRATOR']}>
                            <AuditorPage />
                        </ProtectedLayout>
                    } />

                    <Route path="/admin" element={
                        <ProtectedLayout allowedRoles={['ROLE_ADMINISTRATOR']}>
                            <AdminPage />
                        </ProtectedLayout>
                    } />

                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}
