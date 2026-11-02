import { useState } from 'react';
import { createAdminUser } from '../api/authApi';
import { ApiError } from '../api/httpClient';

const ROLE_OPTIONS = ['TECHNICIAN', 'ADMIN'];

const toMessage = (error) => {
  if (error instanceof ApiError && error.message) {
    return error.message;
  }
  return 'Unable to create this account right now. Please try again.';
};

export default function AdminUserCreatePage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState('TECHNICIAN');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const resetForm = () => {
    setName('');
    setEmail('');
    setPassword('');
    setConfirmPassword('');
    setRole('TECHNICIAN');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccessMessage('');

    if (!name.trim() || !email.trim() || !password || !confirmPassword) {
      setError('All fields are required.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Password confirmation does not match.');
      return;
    }

    if (!ROLE_OPTIONS.includes(role)) {
      setError('Please select a valid role.');
      return;
    }

    setSubmitting(true);
    try {
      const created = await createAdminUser({
        name: name.trim(),
        email: email.trim(),
        password,
        role,
      });

      setSuccessMessage(`Created ${created.role} account for ${created.name}.`);
      resetForm();
    } catch (requestError) {
      setError(toMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="card">
      <h2>Create Support Account</h2>
      <p className="info-text">Create new TECHNICIAN or ADMIN accounts. End-user registration is public.</p>

      {successMessage && (
        <div className="alert success" role="status">
          {successMessage}
        </div>
      )}

      {error && (
        <div className="alert error" role="alert">
          {error}
        </div>
      )}

      <form className="ticket-form" onSubmit={handleSubmit}>
        <label htmlFor="admin-create-name">Name</label>
        <input
          id="admin-create-name"
          type="text"
          value={name}
          onChange={(event) => setName(event.target.value)}
          disabled={submitting}
          maxLength={100}
          required
        />

        <label htmlFor="admin-create-email">Email</label>
        <input
          id="admin-create-email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          disabled={submitting}
          required
        />

        <label htmlFor="admin-create-password">Password</label>
        <input
          id="admin-create-password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          disabled={submitting}
          minLength={8}
          maxLength={72}
          required
        />

        <label htmlFor="admin-create-confirm-password">Confirm password</label>
        <input
          id="admin-create-confirm-password"
          type="password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          disabled={submitting}
          minLength={8}
          maxLength={72}
          required
        />

        <label htmlFor="admin-create-role">Role</label>
        <select
          id="admin-create-role"
          value={role}
          onChange={(event) => setRole(event.target.value)}
          disabled={submitting}
        >
          {ROLE_OPTIONS.map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>

        <div className="ticket-form-actions">
          <button className="primary-btn" type="submit" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create Account'}
          </button>
        </div>
      </form>
    </section>
  );
}

