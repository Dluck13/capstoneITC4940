import ThemeToggleButton from '../components/ThemeToggleButton';
import TicketCoopLogo from '../components/TicketCoopLogo';

function TrackIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 6.5h16M4 12h16M4 17.5h10" />
      <circle cx="18" cy="17.5" r="2.2" />
    </svg>
  );
}

function WorkflowIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="3" y="4" width="6" height="6" rx="1.6" />
      <rect x="15" y="4" width="6" height="6" rx="1.6" />
      <rect x="9" y="14" width="6" height="6" rx="1.6" />
      <path d="M9 7h6M12 10v4" />
    </svg>
  );
}

function ResolveIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="8.2" />
      <path d="M8.5 12.4l2.4 2.5L15.8 10" />
    </svg>
  );
}

export default function HomePage({ isAuthenticated, onGoToRegister, onGoToLogin, onGoToDashboard }) {
  const primaryAction = isAuthenticated
    ? { label: 'Go to Dashboard', action: onGoToDashboard }
    : { label: 'Get Started', action: onGoToRegister };

  const secondaryAction = isAuthenticated ? null : { label: 'Sign In', action: onGoToLogin };

  return (
    <div className="home-shell">
      <div className="home-shell-glow" aria-hidden="true" />
      <div className="home-shell-grid" aria-hidden="true" />

      <header className="home-nav">
        <button type="button" className="home-brand" onClick={onGoToDashboard} aria-label="TicketCoop home">
          <TicketCoopLogo size={34} className="home-brand-logo" title="TicketCoop" />
          <span>
            <span className="brand-word-primary">Ticket</span>
            <span className="brand-word-accent">Coop</span>
          </span>
        </button>

        <div className="home-nav-actions">
          <ThemeToggleButton />
          {secondaryAction && (
            <button type="button" className="secondary-btn" onClick={secondaryAction.action}>
              {secondaryAction.label}
            </button>
          )}
          <button type="button" className="primary-btn" onClick={primaryAction.action}>
            {isAuthenticated ? 'Dashboard' : 'Get Started'}
          </button>
        </div>
      </header>

      <main className="home-main">
        <section className="home-hero">
          <p className="home-badge">SMARTER IT SUPPORT OPERATIONS</p>
          <h1>
            Every Ticket. One Place.
            <span className="home-hero-accent"> Zero Chaos.</span>
          </h1>
          <p className="home-subtitle">
            A unified workspace to manage IT support requests, track progress, and deliver resolutions with confidence.
          </p>

          <div className="home-cta-row">
            <button type="button" className="primary-btn home-cta" onClick={primaryAction.action}>
              {isAuthenticated ? 'Dashboard' : 'Get Started'}
            </button>
            {!isAuthenticated && (
              <button type="button" className="secondary-btn home-cta" onClick={onGoToLogin}>
                Sign In
              </button>
            )}
          </div>
        </section>

        <section className="home-features" aria-label="TicketCoop features">
          <article className="home-feature-card">
            <span className="home-feature-icon" aria-hidden="true">
              <TrackIcon />
            </span>
            <h2>Track Every Request</h2>
            <p>Keep support tickets organized and visible from creation through closure.</p>
          </article>

          <article className="home-feature-card">
            <span className="home-feature-icon" aria-hidden="true">
              <WorkflowIcon />
            </span>
            <h2>Manage Workflows</h2>
            <p>Assign tickets, update progress, and collaborate through a structured support process.</p>
          </article>

          <article className="home-feature-card">
            <span className="home-feature-icon" aria-hidden="true">
              <ResolveIcon />
            </span>
            <h2>Resolve with Confidence</h2>
            <p>Document resolutions and close completed requests with a clear history of activity.</p>
          </article>
        </section>

        <section className="home-flow" aria-label="Ticket lifecycle preview">
          {['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((status, index) => (
            <div className="home-flow-step" key={status}>
              <span>{status}</span>
              {index < 4 ? <i aria-hidden="true">→</i> : null}
            </div>
          ))}
        </section>
      </main>

      <footer className="home-footer">
        <p>
          <strong>TicketCoop</strong> · IT Support Operations Dashboard
        </p>
        <p>© 2026 TicketCoop</p>
      </footer>
    </div>
  );
}
