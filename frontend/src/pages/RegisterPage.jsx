import { useState } from 'react';
import { ApiError } from '../api/httpClient';
import { register as registerRequest } from '../api/authApi';

const toMessage = (error) => {
  if (error instanceof ApiError && error.message) {
    return error.message;
  }
  return 'Unable to create your account right now. Please try again.';
};

export default function RegisterPage({ onBackToLogin, onRegistered }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!name.trim() || !email.trim() || !password || !confirmPassword) {
      setError('All fields are required.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Password confirmation does not match.');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    setSubmitting(true);
    try {
      await registerRequest({ name: name.trim(), email: email.trim(), password });
      onRegistered();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <section className="auth-card">
        <header className="auth-header">
          <h1>Create your account</h1>
          <p>Register a new end-user account for IT support requests.</p>
        </header>

        {error && (
          <div className="alert error" role="alert">
            {error}
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="register-name">Name</label>
          <input
            id="register-name"
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            disabled={submitting}
            maxLength={100}
            required
          />

          <label htmlFor="register-email">Email</label>
          <input
            id="register-email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            disabled={submitting}
            required
          />

          <label htmlFor="register-password">Password</label>
          <input
            id="register-password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={submitting}
            minLength={8}
            maxLength={72}
            required
          />

          <label htmlFor="register-confirm-password">Confirm password</label>
          <input
            id="register-confirm-password"
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            disabled={submitting}
            minLength={8}
            maxLength={72}
            required
          />

          <button type="submit" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create Account'}
          </button>
        </form>

        <div className="auth-footer">
          <span>Already have an account?</span>
          <button type="button" className="text-link-btn" onClick={onBackToLogin} disabled={submitting}>
            Back to sign in
          </button>
        </div>
      </section>
    </div>
  );
}

