import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../api/httpClient';
import { getTickets } from '../api/ticketApi';

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'OPEN', label: 'Open' },
  { value: 'ASSIGNED', label: 'Assigned' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'CLOSED', label: 'Closed' },
];

const STATUS_ORDER = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

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

const normalizeSearch = (value) => value.trim().toLowerCase();

const toSearchableText = (ticket) =>
  [ticket.id, ticket.title, ticket.description]
    .filter((value) => value !== null && value !== undefined)
    .map((value) => String(value).toLowerCase())
    .join(' ');

export default function DashboardPage({ onCreateTicket, onViewTicket }) {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const loadTickets = async ({ asRefresh = false } = {}) => {
    if (asRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    setErrorMessage('');

    try {
      const data = await getTickets();
      setTickets(Array.isArray(data) ? data : []);
    } catch (error) {
      setErrorMessage(toFriendlyError(error));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadTickets();
    }, 0);

    return () => window.clearTimeout(timer);
  }, []);

  const summary = useMemo(() => {
    const counts = {
      TOTAL: tickets.length,
      OPEN: 0,
      ASSIGNED: 0,
      IN_PROGRESS: 0,
      RESOLVED: 0,
      CLOSED: 0,
    };

    for (const ticket of tickets) {
      if (Object.prototype.hasOwnProperty.call(counts, ticket.status)) {
        counts[ticket.status] += 1;
      }
    }

    return counts;
  }, [tickets]);

  const filteredTickets = useMemo(() => {
    const normalizedSearch = normalizeSearch(searchTerm);

    return tickets.filter((ticket) => {
      if (statusFilter !== 'ALL' && ticket.status !== statusFilter) {
        return false;
      }

      if (!normalizedSearch) {
        return true;
      }

      return toSearchableText(ticket).includes(normalizedSearch);
    });
  }, [searchTerm, statusFilter, tickets]);

  const clearFilters = () => {
    setSearchTerm('');
    setStatusFilter('ALL');
  };

  const hasActiveFilters = searchTerm.trim() !== '' || statusFilter !== 'ALL';

  return (
    <section className="dashboard-stack">
      <section className="card">
        <h2>Ticket Summary</h2>
        <p>Counts are based on tickets your current account is authorized to view.</p>

        <div className="stats-grid" aria-label="Ticket statistics">
          <article className="stat-card">
            <h3>Total Tickets</h3>
            <p>{summary.TOTAL}</p>
          </article>
          <article className="stat-card">
            <h3>Open</h3>
            <p>{summary.OPEN}</p>
          </article>
          <article className="stat-card">
            <h3>Assigned</h3>
            <p>{summary.ASSIGNED}</p>
          </article>
          <article className="stat-card">
            <h3>In Progress</h3>
            <p>{summary.IN_PROGRESS}</p>
          </article>
          <article className="stat-card">
            <h3>Resolved</h3>
            <p>{summary.RESOLVED}</p>
          </article>
          <article className="stat-card">
            <h3>Closed</h3>
            <p>{summary.CLOSED}</p>
          </article>
        </div>
      </section>

      <section className="card">
        <div className="card-header-row">
          <div>
            <h2>Tickets</h2>
            <p>Search and filter tickets available to your current role.</p>
          </div>
          <div className="header-actions">
            <button type="button" className="secondary-btn" onClick={() => loadTickets({ asRefresh: true })} disabled={loading || refreshing}>
              {refreshing ? 'Refreshing…' : 'Refresh'}
            </button>
            <button type="button" className="primary-btn" onClick={onCreateTicket}>
              Create Ticket
            </button>
          </div>
        </div>

        <div className="filters-grid" role="search">
          <div>
            <label htmlFor="ticket-search">Search</label>
            <input
              id="ticket-search"
              className="filter-input"
              type="search"
              placeholder="Search by ID, title, or description"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="ticket-status-filter">Status</label>
            <select
              id="ticket-status-filter"
              className="filter-input"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-actions">
            <button type="button" className="secondary-btn" onClick={clearFilters} disabled={!hasActiveFilters}>
              Clear Filters
            </button>
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

        {!loading && !errorMessage && tickets.length > 0 && filteredTickets.length === 0 && (
          <div className="alert warning" role="status">
            No tickets match your current search and filter values.
          </div>
        )}

        {!loading && !errorMessage && filteredTickets.length > 0 && (
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
                {filteredTickets.map((ticket) => (
                  <tr key={ticket.id}>
                    <td data-label="ID">{ticket.id}</td>
                    <td data-label="Title">{valueOrFallback(ticket.title)}</td>
                    <td data-label="Priority">{valueOrFallback(ticket.priority)}</td>
                    <td data-label="Status">
                      <span className={`status-badge status-${String(ticket.status || '').toLowerCase()}`}>
                        {valueOrFallback(ticket.status)}
                      </span>
                    </td>
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

        {!loading && !errorMessage && filteredTickets.length > 0 && (
          <p className="info-text summary-line">
            Showing {filteredTickets.length} of {tickets.length} ticket{tickets.length === 1 ? '' : 's'}.
          </p>
        )}

        {!loading && !errorMessage && tickets.length > 0 && STATUS_ORDER.some((status) => summary[status] === 0) && (
          <p className="info-text">Statuses with zero tickets are still shown in the summary cards.</p>
        )}
      </section>
    </section>
  );
}

