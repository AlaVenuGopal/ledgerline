// frontend/src/components/Sidebar.jsx
import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
    LayoutDashboard, 
    Wallet, 
    ArrowLeftRight, 
    PlusCircle, 
    History, 
    ShieldCheck, 
    Users, 
    LogOut 
} from 'lucide-react';

export default function Sidebar() {
    const { user, logout } = useAuth();

    const isAdmin = user?.role === 'ROLE_ADMINISTRATOR';
    const isAuditor = user?.role === 'ROLE_AUDITOR';
    const isAccountHolder = user?.role === 'ROLE_ACCOUNT_HOLDER';

    const getRoleClass = () => {
        if (isAdmin) return 'role-admin';
        if (isAuditor) return 'role-auditor';
        return 'role-holder';
    };

    const getRoleLabel = () => {
        if (isAdmin) return 'Administrator';
        if (isAuditor) return 'Auditor';
        return 'Account Holder';
    };

    return (
        <aside className="sidebar">
            <div className="sidebar-header">
                <div className="logo-badge">LL</div>
                <div>
                    <div className="logo-text">LedgerLine</div>
                    <div style={{ fontSize: '0.7rem', color: '#6b7280' }}>Provable Money Movement</div>
                </div>
            </div>

            <nav className="sidebar-nav">
                <NavLink to="/" end className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                    <LayoutDashboard size={18} />
                    <span>Dashboard</span>
                </NavLink>

                <NavLink to="/accounts" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                    <Wallet size={18} />
                    <span>Accounts & Balances</span>
                </NavLink>

                {(isAccountHolder || isAdmin) && (
                    <>
                        <NavLink to="/transfer" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                            <ArrowLeftRight size={18} />
                            <span>Transfer Money</span>
                        </NavLink>

                        <NavLink to="/move-money" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                            <PlusCircle size={18} />
                            <span>Deposit & Withdraw</span>
                        </NavLink>
                    </>
                )}

                <NavLink to="/transactions" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                    <History size={18} />
                    <span>Transactions</span>
                </NavLink>

                {(isAuditor || isAdmin) && (
                    <NavLink to="/auditor" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                        <ShieldCheck size={18} />
                        <span>Audit & Ledger Proof</span>
                    </NavLink>
                )}

                {isAdmin && (
                    <NavLink to="/admin" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                        <Users size={18} />
                        <span>User Management</span>
                    </NavLink>
                )}
            </nav>

            <div className="sidebar-footer">
                <div className="user-info">
                    <span className="user-name">{user?.fullName || user?.username}</span>
                    <span className={`user-role-badge ${getRoleClass()}`}>{getRoleLabel()}</span>
                </div>
                <button 
                    onClick={logout} 
                    className="btn btn-secondary" 
                    style={{ padding: '6px 10px' }}
                    title="Log out"
                >
                    <LogOut size={16} />
                </button>
            </div>
        </aside>
    );
}
