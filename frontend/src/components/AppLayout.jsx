import { useState } from 'react';
import { useAuth } from '../auth/useAuth';
import ThemeToggleButton from './ThemeToggleButton';

const BASE_NAV_ITEMS = [
  { key: 'dashboard', label: 'Dashboard', path: '/' },
  { key: 'create-ticket', label: 'Create Ticket', path: '/tickets/new' },
];

const ADMIN_NAV_ITEM = { key: 'admin-users', label: 'Manage Accounts', path: '/admin/users/new' };

export default function AppLayout({ children, onNavigate, currentPath, pageTitle, pageSubtitle }) {
  const { user, logout } = useAuth();
  const [loggingOut, setLoggingOut] = useState(false);
  const navItems = user?.role === 'ADMIN' ? [...BASE_NAV_ITEMS, ADMIN_NAV_ITEM] : BASE_NAV_ITEMS;

  const handleLogout = async () => {
    setLoggingOut(true);
    await logout();
    onNavigate('/login', true);
    setLoggingOut(false);
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">IT Support Operations Dashboard</div>
        <nav className="nav-list" aria-label="Main navigation">
          {navItems.map((item) => {
            const active = currentPath === item.path;
            return (
              <button
                key={item.key}
                type="button"
                className={`nav-item ${active ? 'active' : ''}`}
                onClick={() => onNavigate(item.path)}
                title={item.label}
              >
                {item.label}
              </button>
            );
          })}
        </nav>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <div>
            <h1>{pageTitle || 'Dashboard'}</h1>
            <p className="muted">{pageSubtitle || 'Welcome back to support operations.'}</p>
          </div>

          <div className="topbar-controls">
            <div className="user-details">
              <strong>{user?.name || 'Unknown User'}</strong>
              <span className="role-badge">{user?.role || 'UNKNOWN'}</span>
            </div>
            <ThemeToggleButton />
            <button type="button" className="logout-btn" onClick={handleLogout} disabled={loggingOut}>
              {loggingOut ? 'Signing out…' : 'Logout'}
            </button>
          </div>
        </header>

        <main className="content">{children}</main>
      </div>
    </div>
  );
}
