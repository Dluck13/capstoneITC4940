import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';

export default function LoginPage({ onLoginSuccess }) {
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
      <section className="auth-card">
        <header className="auth-header">
          <h1>IT Support Operations Dashboard</h1>
          <p>Sign in with your support account.</p>
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
      </section>
    </div>
  );
}
