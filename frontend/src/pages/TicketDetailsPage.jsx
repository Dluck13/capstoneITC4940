import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { ApiError } from '../api/httpClient';
import {
  addTicketComment,
  assignTicket,
  closeTicket,
  getTechnicians,
  getTicketById,
  getTicketComments,
  resolveTicket,
  updateTicketStatus,
} from '../api/ticketApi';

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

const operationError = (error, fallback) => {
  if (error instanceof ApiError) {
    if (error.status === 400) {
      return error.message || 'This request is not valid in the current ticket state.';
    }
    if (error.status === 401) {
      return 'Your session expired. Please sign in again.';
    }
    if (error.status === 403) {
      return 'You do not have permission to perform this action.';
    }
    if (error.status === 404) {
      return 'Requested ticket or related record was not found.';
    }
    if (error.status === 409) {
      return error.message || 'Ticket state changed. Refresh and try again.';
    }
    if (error.network) {
      return 'Unable to reach the backend. Verify the API server is running.';
    }
    return error.message || fallback;
  }

  return fallback;
};

const sortCommentsChronologically = (comments) =>
  [...comments].sort((a, b) => {
    const left = new Date(a.createdAt).getTime();
    const right = new Date(b.createdAt).getTime();

    if (Number.isNaN(left) && Number.isNaN(right)) return 0;
    if (Number.isNaN(left)) return -1;
    if (Number.isNaN(right)) return 1;
    return left - right;
  });

export default function TicketDetailsPage({ ticketId, onBack }) {
  const { user } = useAuth();

  const [ticket, setTicket] = useState(null);
  const [loadingTicket, setLoadingTicket] = useState(true);
  const [ticketError, setTicketError] = useState('');

  const [technicians, setTechnicians] = useState([]);
  const [selectedTechnicianId, setSelectedTechnicianId] = useState('');
  const [loadingTechnicians, setLoadingTechnicians] = useState(false);

  const [comments, setComments] = useState([]);
  const [loadingComments, setLoadingComments] = useState(false);
  const [commentError, setCommentError] = useState('');
  const [commentText, setCommentText] = useState('');

  const [assigning, setAssigning] = useState(false);
  const [startingProgress, setStartingProgress] = useState(false);
  const [postingComment, setPostingComment] = useState(false);
  const [resolving, setResolving] = useState(false);
  const [closing, setClosing] = useState(false);

  const [resolutionNotesInput, setResolutionNotesInput] = useState('');
  const [resolutionInputError, setResolutionInputError] = useState('');

  const [actionNotice, setActionNotice] = useState({ type: '', message: '' });

  const userRole = user?.role;
  const isAdmin = userRole === 'ADMIN';
  const isTechnician = userRole === 'TECHNICIAN';

  const canViewAdminAssign = isAdmin && ticket?.status === 'OPEN';
  const canAssignToMe = isTechnician && ticket?.status === 'OPEN' && Boolean(user?.id);
  const canStartProgress =
    ticket?.status === 'ASSIGNED' &&
    (isAdmin || (isTechnician && ticket?.assignedTechnician?.id === user?.id));
  const canResolve =
    ticket?.status === 'IN_PROGRESS' &&
    (isAdmin || (isTechnician && ticket?.assignedTechnician?.id === user?.id));
  const canClose = isAdmin && ticket?.status === 'RESOLVED';
  const canComment = Boolean(ticket) && ticket.status !== 'CLOSED';

  const isBusy = assigning || startingProgress || postingComment || resolving || closing;

  const loadTicket = async () => {
    const data = await getTicketById(ticketId);
    setTicket(data);
    return data;
  };

  const loadComments = async () => {
    const data = await getTicketComments(ticketId);
    const normalized = Array.isArray(data) ? data : [];
    setComments(sortCommentsChronologically(normalized));
  };

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      setLoadingTicket(true);
      setTicketError('');
      setActionNotice({ type: '', message: '' });

      try {
        const data = await getTicketById(ticketId);
        if (!active) return;
        setTicket(data);
      } catch (error) {
        if (!active) return;
        setTicket(null);
        setTicketError(detailError(error));
      } finally {
        if (active) {
          setLoadingTicket(false);
        }
      }
    };

    bootstrap();

    return () => {
      active = false;
    };
  }, [ticketId]);

  useEffect(() => {
    if (!ticket) {
      return;
    }

    let active = true;

    const fetchComments = async () => {
      setLoadingComments(true);
      setCommentError('');

      try {
        const data = await getTicketComments(ticketId);
        if (!active) return;
        const normalized = Array.isArray(data) ? data : [];
        setComments(sortCommentsChronologically(normalized));
      } catch (error) {
        if (!active) return;
        setCommentError(operationError(error, 'Unable to load comments.'));
      } finally {
        if (active) {
          setLoadingComments(false);
        }
      }
    };

    fetchComments();

    return () => {
      active = false;
    };
  }, [ticket, ticketId]);

  useEffect(() => {
    if (!canViewAdminAssign) {
      return;
    }

    let active = true;

    const fetchTechnicians = async () => {
      setLoadingTechnicians(true);

      try {
        const data = await getTechnicians();
        if (!active) return;

        const list = Array.isArray(data) ? data : [];
        setTechnicians(list);
        setSelectedTechnicianId(list[0]?.id ? String(list[0].id) : '');
      } catch {
        if (!active) return;
        setTechnicians([]);
        setSelectedTechnicianId('');
      } finally {
        if (active) {
          setLoadingTechnicians(false);
        }
      }
    };

    fetchTechnicians();

    return () => {
      active = false;
    };
  }, [canViewAdminAssign]);

  const refreshTicketAndComments = async () => {
    const updated = await loadTicket();
    await loadComments();
    return updated;
  };

  const handleAssignToTechnician = async () => {
    if (!selectedTechnicianId) {
      setActionNotice({ type: 'error', message: 'Select a technician before assigning.' });
      return;
    }

    setActionNotice({ type: '', message: '' });
    setAssigning(true);

    try {
      await assignTicket(ticketId, { technicianId: Number(selectedTechnicianId) });
      await refreshTicketAndComments();
      setActionNotice({ type: 'success', message: 'Ticket assigned successfully.' });
    } catch (error) {
      setActionNotice({ type: 'error', message: operationError(error, 'Unable to assign ticket.') });
    } finally {
      setAssigning(false);
    }
  };

  const handleAssignToMe = async () => {
    if (!user?.id) {
      setActionNotice({ type: 'error', message: 'Unable to identify your account for assignment.' });
      return;
    }

    setActionNotice({ type: '', message: '' });
    setAssigning(true);

    try {
      await assignTicket(ticketId, { technicianId: Number(user.id) });
      await refreshTicketAndComments();
      setActionNotice({ type: 'success', message: 'Ticket assigned to you.' });
    } catch (error) {
      setActionNotice({ type: 'error', message: operationError(error, 'Unable to assign ticket to yourself.') });
    } finally {
      setAssigning(false);
    }
  };

  const handleStartProgress = async () => {
    setActionNotice({ type: '', message: '' });
    setStartingProgress(true);

    try {
      await updateTicketStatus(ticketId, { status: 'IN_PROGRESS' });
      await refreshTicketAndComments();
      setActionNotice({ type: 'success', message: 'Ticket moved to IN_PROGRESS.' });
    } catch (error) {
      setActionNotice({ type: 'error', message: operationError(error, 'Unable to start progress.') });
    } finally {
      setStartingProgress(false);
    }
  };

  const handleSubmitComment = async (event) => {
    event.preventDefault();

    if (!commentText.trim()) {
      setCommentError('Comment text is required.');
      return;
    }

    setActionNotice({ type: '', message: '' });
    setCommentError('');
    setPostingComment(true);

    try {
      await addTicketComment(ticketId, { text: commentText.trim() });
      setCommentText('');
      await loadComments();
      setActionNotice({ type: 'success', message: 'Comment added.' });
    } catch (error) {
      setCommentError(operationError(error, 'Unable to add comment.'));
    } finally {
      setPostingComment(false);
    }
  };

  const handleResolveTicket = async () => {
    if (!resolutionNotesInput.trim()) {
      setResolutionInputError('Resolution notes are required.');
      return;
    }

    setActionNotice({ type: '', message: '' });
    setResolutionInputError('');
    setResolving(true);

    try {
      await resolveTicket(ticketId, { resolutionNotes: resolutionNotesInput.trim() });
      await refreshTicketAndComments();
      setActionNotice({ type: 'success', message: 'Ticket resolved successfully.' });
      setResolutionNotesInput('');
    } catch (error) {
      setActionNotice({ type: 'error', message: operationError(error, 'Unable to resolve this ticket.') });
    } finally {
      setResolving(false);
    }
  };

  const handleCloseTicket = async () => {
    setActionNotice({ type: '', message: '' });
    setClosing(true);

    try {
      await closeTicket(ticketId);
      await refreshTicketAndComments();
      setActionNotice({ type: 'success', message: 'Ticket closed successfully.' });
    } catch (error) {
      setActionNotice({ type: 'error', message: operationError(error, 'Unable to close this ticket.') });
    } finally {
      setClosing(false);
    }
  };

  const infoItems = useMemo(
    () => [
      { label: 'ID', value: valueOrFallback(ticket?.id) },
      { label: 'Title', value: valueOrFallback(ticket?.title) },
      { label: 'Priority', value: valueOrFallback(ticket?.priority) },
      {
        label: 'Status',
        value: (
          <span className={`status-badge status-${String(ticket?.status || '').toLowerCase()}`}>
            {valueOrFallback(ticket?.status)}
          </span>
        ),
      },
      { label: 'Category', value: valueOrFallback(ticket?.category?.name) },
      { label: 'Requester', value: valueOrFallback(ticket?.requester?.name) },
      {
        label: 'Assigned Technician',
        value: valueOrFallback(ticket?.assignedTechnician?.name, 'Unassigned'),
      },
      { label: 'Created At', value: formatDateTime(ticket?.createdAt) },
      { label: 'Updated At', value: formatDateTime(ticket?.updatedAt) },
      { label: 'Resolved At', value: formatDateTime(ticket?.resolvedAt) },
    ],
    [ticket],
  );

  return (
    <section className="dashboard-stack">
      <section className="card">
        <div className="card-header-row">
          <div>
            <h2>Ticket Details</h2>
            <p>Review ticket metadata, timeline, actions, and collaboration updates.</p>
          </div>
          <button type="button" className="secondary-btn" onClick={onBack}>
            Back to Dashboard
          </button>
        </div>

        {loadingTicket && <p className="info-text">Loading ticket details…</p>}

        {!loadingTicket && ticketError && (
          <div className="alert error" role="alert">
            {ticketError}
          </div>
        )}

        {!loadingTicket && !ticketError && ticket && (
          <>
            {actionNotice.message && (
              <div className={`alert ${actionNotice.type === 'error' ? 'error' : 'success'}`} role="status">
                {actionNotice.message}
              </div>
            )}

            <section className="action-panel">
              <h3>Ticket Actions</h3>

              {canViewAdminAssign && (
                <div className="action-row">
                  <label htmlFor="technician-select">Assign to technician</label>
                  <select
                    id="technician-select"
                    value={selectedTechnicianId}
                    onChange={(event) => setSelectedTechnicianId(event.target.value)}
                    disabled={isBusy || loadingTechnicians || technicians.length === 0}
                  >
                    {technicians.length === 0 && <option value="">No active technicians</option>}
                    {technicians.map((technician) => (
                      <option key={technician.id} value={technician.id}>
                        {technician.name}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="primary-btn"
                    onClick={handleAssignToTechnician}
                    disabled={isBusy || loadingTechnicians || technicians.length === 0 || !selectedTechnicianId}
                  >
                    {assigning ? 'Assigning…' : 'Assign Ticket'}
                  </button>
                </div>
              )}

              {canAssignToMe && (
                <div className="action-row">
                  <button type="button" className="primary-btn" onClick={handleAssignToMe} disabled={isBusy}>
                    {assigning ? 'Assigning…' : 'Assign to Me'}
                  </button>
                </div>
              )}

              {canStartProgress && (
                <div className="action-row">
                  <button type="button" className="primary-btn" onClick={handleStartProgress} disabled={isBusy}>
                    {startingProgress ? 'Updating…' : 'Start Progress'}
                  </button>
                </div>
              )}

              {canResolve && (
                <div className="action-stack">
                  <label htmlFor="resolution-notes">Resolution notes</label>
                  <textarea
                    id="resolution-notes"
                    rows={4}
                    maxLength={10000}
                    value={resolutionNotesInput}
                    onChange={(event) => setResolutionNotesInput(event.target.value)}
                    disabled={isBusy}
                  />
                  {resolutionInputError && <p className="field-error">{resolutionInputError}</p>}
                  <div className="action-row">
                    <button
                      type="button"
                      className="primary-btn"
                      onClick={handleResolveTicket}
                      disabled={isBusy}
                    >
                      {resolving ? 'Resolving…' : 'Resolve Ticket'}
                    </button>
                  </div>
                </div>
              )}

              {canClose && (
                <div className="action-row">
                  <button type="button" className="primary-btn" onClick={handleCloseTicket} disabled={isBusy}>
                    {closing ? 'Closing…' : 'Close Ticket'}
                  </button>
                </div>
              )}

              {!canViewAdminAssign &&
                !canAssignToMe &&
                !canStartProgress &&
                !canResolve &&
                !canClose && (
                  <p className="info-text">No ticket actions are available for your role at this ticket state.</p>
                )}
            </section>

            <section className="description-card">
              <h3>Ticket Information</h3>
              <div className="detail-grid">
                {infoItems.map((item) => (
                  <div key={item.label}>
                    <span className="detail-label">{item.label}</span>
                    <span>{item.value}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="description-card">
              <h3>Description</h3>
              <p>{valueOrFallback(ticket.description)}</p>
            </section>

            {ticket.resolutionNotes && (
              <section className="description-card">
                <h3>Resolution</h3>
                <p>{ticket.resolutionNotes}</p>
              </section>
            )}

            <section className="description-card">
              <h3>Comments</h3>

              {loadingComments && <p className="info-text">Loading comments…</p>}

              {!loadingComments && commentError && (
                <div className="alert error" role="alert">
                  {commentError}
                </div>
              )}

              {!loadingComments && !commentError && comments.length === 0 && (
                <p className="info-text">No comments yet.</p>
              )}

              {!loadingComments && !commentError && comments.length > 0 && (
                <ul className="comment-list">
                  {comments.map((comment) => (
                    <li key={comment.id} className="comment-item">
                      <div className="comment-meta">
                        <strong>{valueOrFallback(comment.authorName, 'Unknown User')}</strong>
                        <span>{formatDateTime(comment.createdAt)}</span>
                      </div>
                      <p>{valueOrFallback(comment.text)}</p>
                    </li>
                  ))}
                </ul>
              )}

              <form className="comment-form" onSubmit={handleSubmitComment}>
                <label htmlFor="comment-text">Add Comment</label>
                <textarea
                  id="comment-text"
                  value={commentText}
                  onChange={(event) => setCommentText(event.target.value)}
                  rows={4}
                  maxLength={10000}
                  disabled={postingComment || !canComment || isBusy}
                />
                {!canComment && (
                  <p className="info-text">Comments are read-only after a ticket is closed.</p>
                )}
                <div className="comment-actions">
                  <button
                    type="submit"
                    className="primary-btn"
                    disabled={postingComment || !canComment || isBusy}
                  >
                    {postingComment ? 'Posting…' : 'Post Comment'}
                  </button>
                </div>
              </form>
            </section>
          </>
        )}
      </section>
    </section>
  );
}

