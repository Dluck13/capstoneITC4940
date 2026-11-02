import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../api/httpClient';
import { createTicket, getCategories } from '../api/ticketApi';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

const toFriendlyError = (error, fallback) => {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return 'Your session expired. Please sign in again.';
    }
    if (error.status === 403) {
      return 'You do not have permission to perform this action.';
    }
    if (error.status === 400) {
      return error.message || 'Please review the form values and try again.';
    }
    if (error.network) {
      return 'Unable to reach the backend. Verify the API server is running.';
    }
    return error.message || fallback;
  }

  return fallback;
};

const initialForm = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  categoryId: '',
};

const buildValidationErrors = (form) => {
  const errors = {};

  if (!form.title.trim()) {
    errors.title = 'Title is required.';
  }

  if (!form.description.trim()) {
    errors.description = 'Description is required.';
  }

  if (!form.priority) {
    errors.priority = 'Priority is required.';
  }

  if (!form.categoryId) {
    errors.categoryId = 'Category is required.';
  }

  return errors;
};

export default function CreateTicketPage({ onCancel, onCreated }) {
  const [form, setForm] = useState(initialForm);
  const [categories, setCategories] = useState([]);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');

  const hasCategories = categories.length > 0;

  const categoryOptions = useMemo(
    () => categories.map((category) => ({ id: String(category.id), name: category.name })),
    [categories],
  );

  useEffect(() => {
    let active = true;

    const loadCategories = async () => {
      setLoadingCategories(true);
      setErrorMessage('');

      try {
        const data = await getCategories();
        if (!active) return;

        const list = Array.isArray(data) ? data : [];
        setCategories(list);
        if (list.length > 0) {
          setForm((prev) => ({
            ...prev,
            categoryId: prev.categoryId || String(list[0].id),
          }));
        }
      } catch (error) {
        if (!active) return;
        setErrorMessage(toFriendlyError(error, 'Unable to load categories.'));
      } finally {
        if (active) {
          setLoadingCategories(false);
        }
      }
    };

    loadCategories();

    return () => {
      active = false;
    };
  }, []);

  const setField = (name, value) => {
    setForm((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => {
      if (!prev[name]) {
        return prev;
      }

      const next = { ...prev };
      delete next[name];
      return next;
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage('');

    const validationErrors = buildValidationErrors(form);
    setFieldErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setSubmitting(true);

    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        priority: form.priority,
        categoryId: Number(form.categoryId),
      };

      const createdTicket = await createTicket(payload);
      const createdId = createdTicket?.id;

      if (!createdId) {
        setErrorMessage('Ticket was created, but the response was missing the ticket ID.');
        return;
      }

      onCreated(createdId);
    } catch (error) {
      setErrorMessage(toFriendlyError(error, 'Unable to create ticket right now.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="dashboard-stack">
      <section className="card">
        <div className="card-header-row">
          <div>
            <h2>Create Ticket</h2>
            <p>Submit a new support request for the operations team.</p>
          </div>
          <button type="button" className="secondary-btn" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>

        {errorMessage && (
          <div className="alert error" role="alert">
            {errorMessage}
          </div>
        )}

        {loadingCategories ? (
          <p className="info-text">Loading categories…</p>
        ) : !hasCategories ? (
          <div className="alert warning" role="alert">
            No active categories are available. Please contact an administrator.
          </div>
        ) : (
          <form className="ticket-form" onSubmit={handleSubmit} noValidate>
            <label htmlFor="ticket-title">Title</label>
            <input
              id="ticket-title"
              type="text"
              maxLength={150}
              value={form.title}
              onChange={(event) => setField('title', event.target.value)}
              disabled={submitting}
            />
            {fieldErrors.title && <p className="field-error">{fieldErrors.title}</p>}

            <label htmlFor="ticket-description">Description</label>
            <textarea
              id="ticket-description"
              rows={6}
              maxLength={10000}
              value={form.description}
              onChange={(event) => setField('description', event.target.value)}
              disabled={submitting}
            />
            {fieldErrors.description && <p className="field-error">{fieldErrors.description}</p>}

            <div className="ticket-form-row">
              <div>
                <label htmlFor="ticket-priority">Priority</label>
                <select
                  id="ticket-priority"
                  value={form.priority}
                  onChange={(event) => setField('priority', event.target.value)}
                  disabled={submitting}
                >
                  {PRIORITIES.map((priority) => (
                    <option key={priority} value={priority}>
                      {priority}
                    </option>
                  ))}
                </select>
                {fieldErrors.priority && <p className="field-error">{fieldErrors.priority}</p>}
              </div>

              <div>
                <label htmlFor="ticket-category">Category</label>
                <select
                  id="ticket-category"
                  value={form.categoryId}
                  onChange={(event) => setField('categoryId', event.target.value)}
                  disabled={submitting}
                >
                  {categoryOptions.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
                {fieldErrors.categoryId && <p className="field-error">{fieldErrors.categoryId}</p>}
              </div>
            </div>

            <div className="ticket-form-actions">
              <button type="button" className="secondary-btn" onClick={onCancel} disabled={submitting}>
                Cancel
              </button>
              <button type="submit" className="primary-btn" disabled={submitting}>
                {submitting ? 'Creating…' : 'Create Ticket'}
              </button>
            </div>
          </form>
        )}
      </section>
    </section>
  );
}
