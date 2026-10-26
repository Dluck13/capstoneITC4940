import { useState } from 'react';
import { useAuth } from '../auth/useAuth';

const NAV_ITEMS = [
  { key: 'dashboard', label: 'Dashboard', path: '/', disabled: false },
  { key: 'create-ticket', label: 'Create Ticket', path: '#', disabled: true },
];

export default function AppLayout({ children, onNavigate, currentPath, pageTitle, pageSubtitle }) {
  const { user, logout } = useAuth();
  const [loggingOut, setLoggingOut] = useState(false);

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
          {NAV_ITEMS.map((item) => {
            const active = !item.disabled && currentPath === item.path;
            return (
              <button
                key={item.key}
                type="button"
                className={`nav-item ${active ? 'active' : ''}`}
                disabled={item.disabled}
                onClick={() => onNavigate(item.path)}
                title={item.disabled ? 'Coming soon' : item.label}
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
          <div className="user-info">
            <div className="user-details">
              <strong>{user?.name || 'Unknown User'}</strong>
              <span className="role-badge">{user?.role || 'UNKNOWN'}</span>
            </div>
            <button
              type="button"
              className="logout-btn"
              onClick={handleLogout}
              disabled={loggingOut}
            >
              {loggingOut ? 'Signing out…' : 'Logout'}
            </button>
          </div>
        </header>

        <main className="content">{children}</main>
      </div>
    </div>
  );
}