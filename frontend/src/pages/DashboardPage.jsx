import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../api/httpClient';
import { getTickets } from '../api/ticketApi';

const formatDateTime = (value) => {
  if (!value) return '—';

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
};

const toFriendlyError = (error) => {
  if (error instanceof ApiError) {
    if (error.status === 403) {
      return 'You do not have permission to view this ticket list.';
    }
    if (error.status === 401) {
      return 'Your session expired. Please sign in again.';
    }
    if (error.network) {
      return 'Unable to reach the backend. Verify the API server is running.';
    }
    return error.message || 'Unable to load tickets.';
  }

  return 'Unable to load tickets.';
};

const valueOrFallback = (value, fallback = '—') => {
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  return value;
};

export default function DashboardPage({ onViewTicket }) {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let active = true;

    const loadTickets = async () => {
      setLoading(true);
      setErrorMessage('');

      try {
        const data = await getTickets();
        if (!active) return;
        setTickets(Array.isArray(data) ? data : []);
      } catch (error) {
        if (!active) return;
        setErrorMessage(toFriendlyError(error));
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadTickets();

    return () => {
      active = false;
    };
  }, []);

  const totals = useMemo(() => {
    const initial = {
      OPEN: 0,
      ASSIGNED: 0,
      IN_PROGRESS: 0,
      RESOLVED: 0,
      CLOSED: 0,
    };

    for (const ticket of tickets) {
      if (Object.prototype.hasOwnProperty.call(initial, ticket.status)) {
        initial[ticket.status] += 1;
      }
    }

    return initial;
  }, [tickets]);

  return (
    <section className="dashboard-stack">
      <section className="card">
        <h2>Ticket Overview</h2>
        <p>Live data from the support ticket retrieval API.</p>
        <div className="status-grid">
          <article>
            <h3>Open</h3>
            <p>{totals.OPEN}</p>
          </article>
          <article>
            <h3>Assigned</h3>
            <p>{totals.ASSIGNED}</p>
          </article>
          <article>
            <h3>In Progress</h3>
            <p>{totals.IN_PROGRESS}</p>
          </article>
          <article>
            <h3>Resolved</h3>
            <p>{totals.RESOLVED}</p>
          </article>
          <article>
            <h3>Closed</h3>
            <p>{totals.CLOSED}</p>
          </article>
        </div>
      </section>

      <section className="card">
        <div className="card-header-row">
          <div>
            <h2>Tickets</h2>
            <p>Showing ticket list results available to your current role.</p>
          </div>
        </div>

        {loading && <p className="info-text">Loading tickets…</p>}

        {!loading && errorMessage && (
          <div className="alert error" role="alert">
            {errorMessage}
          </div>
        )}

        {!loading && !errorMessage && tickets.length === 0 && (
          <p className="info-text">No tickets are currently visible for your account.</p>
        )}

        {!loading && !errorMessage && tickets.length > 0 && (
          <div className="ticket-table-wrap">
            <table className="ticket-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Category</th>
                  <th>Requester</th>
                  <th>Assigned To</th>
                  <th>Created</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((ticket) => (
                  <tr key={ticket.id}>
                    <td data-label="ID">{ticket.id}</td>
                    <td data-label="Title">{valueOrFallback(ticket.title)}</td>
                    <td data-label="Priority">{valueOrFallback(ticket.priority)}</td>
                    <td data-label="Status">{valueOrFallback(ticket.status)}</td>
                    <td data-label="Category">{valueOrFallback(ticket.category?.name)}</td>
                    <td data-label="Requester">{valueOrFallback(ticket.requester?.name)}</td>
                    <td data-label="Assigned To">
                      {valueOrFallback(ticket.assignedTechnician?.name, 'Unassigned')}
                    </td>
                    <td data-label="Created">{formatDateTime(ticket.createdAt)}</td>
                    <td data-label="Action">
                      <button
                        type="button"
                        className="table-link"
                        onClick={() => onViewTicket(ticket.id)}
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </section>
  );
}