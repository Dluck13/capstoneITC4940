import { useEffect, useState } from 'react';
import { ApiError } from '../api/httpClient';
import { getTicketById } from '../api/ticketApi';

const formatDateTime = (value) => {
  if (!value) return '—';

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
};

const valueOrFallback = (value, fallback = '—') => {
  if (value === null || value === undefined || value === '') {
    return fallback;
  }
  return value;
};

const detailError = (error) => {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return 'Ticket not found.';
    }
    if (error.status === 403) {
      return 'You do not have permission to view this ticket.';
    }
    if (error.status === 401) {
      return 'Your session expired. Please sign in again.';
    }
    if (error.network) {
      return 'Unable to reach the backend. Verify the API server is running.';
    }
    return error.message || 'Unable to load ticket details.';
  }

  return 'Unable to load ticket details.';
};

export default function TicketDetailsPage({ ticketId, onBack }) {
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let active = true;

    const loadTicket = async () => {
      setLoading(true);
      setErrorMessage('');

      try {
        const data = await getTicketById(ticketId);
        if (!active) return;
        setTicket(data);
      } catch (error) {
        if (!active) return;
        setTicket(null);
        setErrorMessage(detailError(error));
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadTicket();

    return () => {
      active = false;
    };
  }, [ticketId]);

  return (
    <section className="dashboard-stack">
      <section className="card">
        <div className="card-header-row">
          <div>
            <h2>Ticket Details</h2>
          </div>
          <button type="button" className="secondary-btn" onClick={onBack}>
            Back to Dashboard
          </button>
        </div>

        {loading && <p className="info-text">Loading ticket #{ticketId}…</p>}

        {!loading && errorMessage && (
          <div className="alert error" role="alert">
            {errorMessage}
          </div>
        )}

        {!loading && !errorMessage && ticket && (
          <>
            <div className="detail-grid">
              <div>
                <span className="detail-label">ID</span>
                <span>{valueOrFallback(ticket.id)}</span>
              </div>
              <div>
                <span className="detail-label">Title</span>
                <span>{valueOrFallback(ticket.title)}</span>
              </div>
              <div>
                <span className="detail-label">Priority</span>
                <span>{valueOrFallback(ticket.priority)}</span>
              </div>
              <div>
                <span className="detail-label">Status</span>
                <span>{valueOrFallback(ticket.status)}</span>
              </div>
              <div>
                <span className="detail-label">Category</span>
                <span>{valueOrFallback(ticket.category?.name)}</span>
              </div>
              <div>
                <span className="detail-label">Requester</span>
                <span>{valueOrFallback(ticket.requester?.name)}</span>
              </div>
              <div>
                <span className="detail-label">Assigned Technician</span>
                <span>{valueOrFallback(ticket.assignedTechnician?.name, 'Unassigned')}</span>
              </div>
              <div>
                <span className="detail-label">Created At</span>
                <span>{formatDateTime(ticket.createdAt)}</span>
              </div>
              <div>
                <span className="detail-label">Updated At</span>
                <span>{formatDateTime(ticket.updatedAt)}</span>
              </div>
              <div>
                <span className="detail-label">Resolved At</span>
                <span>{formatDateTime(ticket.resolvedAt)}</span>
              </div>
            </div>

            <section className="description-card">
              <h3>Description</h3>
              <p>{valueOrFallback(ticket.description)}</p>
            </section>

            {ticket.resolutionNotes && (
              <section className="description-card">
                <h3>Resolution Notes</h3>
                <p>{ticket.resolutionNotes}</p>
              </section>
            )}
          </>
        )}
      </section>
    </section>
  );
}
