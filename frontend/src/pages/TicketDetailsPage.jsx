import { useEffect, useState } from 'react';
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

  const [actionMessage, setActionMessage] = useState('');

  const userRole = user?.role;
  const isAdmin = userRole === 'ADMIN';
  const isTechnician = userRole === 'TECHNICIAN';

  const showAdminAssign = isAdmin && ticket?.status === 'OPEN';
  const showTechnicianAssignToMe = isTechnician && ticket?.status === 'OPEN';
  const showStartProgress =
    (isAdmin || isTechnician) &&
    ticket?.status === 'ASSIGNED' &&
    Boolean(ticket?.assignedTechnician?.id);

  const showResolve = (isAdmin || isTechnician) && ticket?.status === 'IN_PROGRESS';
  const showClose = isAdmin && ticket?.status === 'RESOLVED';

  const canComment = Boolean(ticket) && ticket.status !== 'CLOSED';

  const isBusy = assigning || startingProgress || postingComment || resolving || closing;

  const loadTicket = async () => {
    const data = await getTicketById(ticketId);
    setTicket(data);
    return data;
  };

  const loadComments = async () => {
    const data = await getTicketComments(ticketId);
    setComments(Array.isArray(data) ? data : []);
  };

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      setLoadingTicket(true);
      setTicketError('');
      setActionMessage('');

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
        setComments(Array.isArray(data) ? data : []);
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
    if (!showAdminAssign) {
      return;
    }

    let active = true;

    const fetchTechnicians = async () => {
      setLoadingTechnicians(true);
      setActionMessage('');

      try {
        const data = await getTechnicians();
        if (!active) return;

        const list = Array.isArray(data) ? data : [];
        setTechnicians(list);
        setSelectedTechnicianId((current) => {
          if (current && list.some((tech) => String(tech.id) === current)) {
            return current;
          }
          return list.length > 0 ? String(list[0].id) : '';
        });
      } catch (error) {
        if (!active) return;
        setActionMessage(operationError(error, 'Unable to load technicians.'));
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
  }, [showAdminAssign]);

  const refreshTicketAndComments = async () => {
    await loadTicket();
    await loadComments();
  };

  const handleAdminAssign = async () => {
    if (!selectedTechnicianId) {
      setActionMessage('Please select a technician before assigning this ticket.');
      return;
    }

    setAssigning(true);
    setActionMessage('');

    try {
      await assignTicket(ticketId, { technicianId: Number(selectedTechnicianId) });
      await refreshTicketAndComments();
    } catch (error) {
      setActionMessage(operationError(error, 'Unable to assign this ticket.'));
    } finally {
      setAssigning(false);
    }
  };

  const handleAssignToMe = async () => {
    if (!user?.id) {
      setActionMessage('Unable to determine your account for assignment.');
      return;
    }

    setAssigning(true);
    setActionMessage('');

    try {
      await assignTicket(ticketId, { technicianId: user.id });
      await refreshTicketAndComments();
    } catch (error) {
      setActionMessage(operationError(error, 'Unable to assign this ticket.'));
    } finally {
      setAssigning(false);
    }
  };

  const handleStartProgress = async () => {
    setStartingProgress(true);
    setActionMessage('');

    try {
      await updateTicketStatus(ticketId, { status: 'IN_PROGRESS' });
      await refreshTicketAndComments();
    } catch (error) {
      setActionMessage(operationError(error, 'Unable to start progress for this ticket.'));
    } finally {
      setStartingProgress(false);
    }
  };

  const handleResolveTicket = async () => {
    const notes = resolutionNotesInput.trim();

    if (!notes) {
      setResolutionInputError('Resolution notes are required.');
      return;
    }

    setResolving(true);
    setActionMessage('');
    setResolutionInputError('');

    try {
      await resolveTicket(ticketId, { resolutionNotes: notes });
      setResolutionNotesInput('');
      await refreshTicketAndComments();
    } catch (error) {
      setActionMessage(operationError(error, 'Unable to resolve this ticket.'));
    } finally {
      setResolving(false);
    }
  };

  const handleCloseTicket = async () => {
    setClosing(true);
    setActionMessage('');

    try {
      await closeTicket(ticketId);
      await refreshTicketAndComments();
    } catch (error) {
      setActionMessage(operationError(error, 'Unable to close this ticket.'));
    } finally {
      setClosing(false);
    }
  };

  const handleSubmitComment = async (event) => {
    event.preventDefault();

    const text = commentText.trim();
    if (!text) {
      setCommentError('Comment text is required.');
      return;
    }

    setPostingComment(true);
    setCommentError('');

    try {
      await addTicketComment(ticketId, { text });
      setCommentText('');
      await loadComments();
      await loadTicket();
    } catch (error) {
      setCommentError(operationError(error, 'Unable to post comment.'));
    } finally {
      setPostingComment(false);
    }
  };

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

        {loadingTicket && <p className="info-text">Loading ticket #{ticketId}…</p>}

        {!loadingTicket && ticketError && (
          <div className="alert error" role="alert">
            {ticketError}
          </div>
        )}

        {!loadingTicket && !ticketError && ticket && (
          <>
            {(showAdminAssign || showTechnicianAssignToMe || showStartProgress || showResolve || showClose || actionMessage) && (
              <section className="action-panel">
                <h3>Actions</h3>

                {actionMessage && (
                  <div className="alert error" role="alert">
                    {actionMessage}
                  </div>
                )}

                {showAdminAssign && (
                  <div className="action-row">
                    <label htmlFor="assign-technician">Assign Technician</label>
                    <select
                      id="assign-technician"
                      value={selectedTechnicianId}
                      onChange={(event) => setSelectedTechnicianId(event.target.value)}
                      disabled={loadingTechnicians || isBusy}
                    >
                      {technicians.map((technician) => (
                        <option key={technician.id} value={technician.id}>
                          {technician.name}
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      className="primary-btn"
                      onClick={handleAdminAssign}
                      disabled={loadingTechnicians || isBusy || technicians.length === 0}
                    >
                      {assigning ? 'Assigning…' : 'Assign Ticket'}
                    </button>
                  </div>
                )}

                {showTechnicianAssignToMe && (
                  <div className="action-row">
                    <button
                      type="button"
                      className="primary-btn"
                      onClick={handleAssignToMe}
                      disabled={isBusy}
                    >
                      {assigning ? 'Assigning…' : 'Assign to Me'}
                    </button>
                  </div>
                )}

                {showStartProgress && (
                  <div className="action-row">
                    <button
                      type="button"
                      className="primary-btn"
                      onClick={handleStartProgress}
                      disabled={isBusy}
                    >
                      {startingProgress ? 'Starting…' : 'Start Progress'}
                    </button>
                  </div>
                )}

                {showResolve && (
                  <div className="action-stack">
                    <label htmlFor="resolution-notes">Resolution Notes</label>
                    <textarea
                      id="resolution-notes"
                      value={resolutionNotesInput}
                      onChange={(event) => {
                        setResolutionNotesInput(event.target.value);
                        if (resolutionInputError) {
                          setResolutionInputError('');
                        }
                      }}
                      rows={4}
                      maxLength={10000}
                      disabled={isBusy}
                    />
                    {resolutionInputError && <p className="field-error">{resolutionInputError}</p>}
                    <button
                      type="button"
                      className="primary-btn"
                      onClick={handleResolveTicket}
                      disabled={isBusy}
                    >
                      {resolving ? 'Resolving…' : 'Resolve Ticket'}
                    </button>
                  </div>
                )}

                {showClose && (
                  <div className="action-row">
                    <button
                      type="button"
                      className="primary-btn"
                      onClick={handleCloseTicket}
                      disabled={isBusy}
                    >
                      {closing ? 'Closing…' : 'Close Ticket'}
                    </button>
                  </div>
                )}
              </section>
            )}

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
