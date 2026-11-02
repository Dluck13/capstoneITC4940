import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import ThemeToggleButton from '../components/ThemeToggleButton';
import TicketCoopLogo from '../components/TicketCoopLogo';

export default function LoginPage({ onLoginSuccess, onCreateAccount, onBackHome }) {
  const { login, isAuthenticated, sessionExpired, clearSessionExpired, globalError, clearGlobalError } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      onLoginSuccess();
    }
  }, [isAuthenticated, onLoginSuccess]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError('');
    clearGlobalError();

    if (!email.trim() || !password.trim()) {
      setFormError('Please enter both email and password.');
      return;
    }

    setSubmitting(true);
    const result = await login({ email: email.trim(), password });
    setSubmitting(false);

    if (!result.success) {
      setFormError(result.message);
      return;
    }

    clearSessionExpired();
    onLoginSuccess();
  };

  return (
    <div className="auth-shell">
      <div className="auth-theme-toggle-wrap">
        <ThemeToggleButton />
      </div>

      <div className="auth-container">
        <header className="auth-branding" aria-label="TicketCoop branding">
          <TicketCoopLogo size={126} />
          <h1><span className="brand-word-primary">Ticket</span><span className="brand-word-accent">Coop</span></h1>
          <p>IT Support Operations Dashboard</p>
        </header>

        <section className="auth-card">
          <header className="auth-header">
            <h2>Sign In</h2>
            <p>Access your TicketCoop account.</p>
          </header>

          {sessionExpired && (
            <div className="alert warning" role="status">
              Your session expired. Please sign in again.
            </div>
          )}

          {globalError && (
            <div className="alert error" role="alert">
              {globalError}
            </div>
          )}

          {formError && (
            <div className="alert error" role="alert">
              {formError}
            </div>
          )}

          <form className="auth-form" onSubmit={handleSubmit}>
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="username"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={submitting}
              required
            />

            <label htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              disabled={submitting}
              required
            />

            <button type="submit" disabled={submitting}>
              {submitting ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          <div className="auth-footer">
            <span>New here?</span>
            <button type="button" className="text-link-btn" onClick={onCreateAccount} disabled={submitting}>
              Create an account
            </button>
            <span aria-hidden="true">·</span>
            <button type="button" className="text-link-btn" onClick={onBackHome} disabled={submitting}>
              Back to home
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}

