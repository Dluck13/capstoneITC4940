import { useCallback, useEffect, useMemo, useState } from 'react';
import './App.css';
import { useAuth } from './auth/useAuth';
import AppLayout from './components/AppLayout';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import TicketDetailsPage from './pages/TicketDetailsPage';

const TICKET_ROUTE = /^\/tickets\/(\d+)$/;

const isValidPath = (path) => path === '/' || path === '/login' || TICKET_ROUTE.test(path);

const sanitizePath = (path) => (isValidPath(path) ? path : '/');

const getPath = () => sanitizePath(window.location.pathname || '/');

const parseRoute = (path) => {
  if (path === '/login') {
    return { type: 'login' };
  }

  if (path === '/') {
    return { type: 'dashboard' };
  }

  const match = path.match(TICKET_ROUTE);
  if (match) {
    return {
      type: 'ticket-details',
      ticketId: Number(match[1]),
    };
  }

  return { type: 'dashboard' };
};

function FullScreenMessage({ title, message }) {
  return (
    <div className="centered-state">
      <h1>{title}</h1>
      <p>{message}</p>
    </div>
  );
}

function App() {
  const { isLoading, isAuthenticated } = useAuth();
  const [currentPath, setCurrentPath] = useState(getPath());

  useEffect(() => {
    const handlePopState = () => setCurrentPath(getPath());
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigate = useCallback((path, replace = false) => {
    const safePath = sanitizePath(path);
    const method = replace ? 'replaceState' : 'pushState';
    window.history[method]({}, '', safePath);
    setCurrentPath(safePath);
  }, []);

  let effectivePath = currentPath;
  if (!isAuthenticated && effectivePath !== '/login') {
    effectivePath = '/login';
  } else if (isAuthenticated && effectivePath === '/login') {
    effectivePath = '/';
  }

  if (!isLoading && window.location.pathname !== effectivePath) {
    window.history.replaceState({}, '', effectivePath);
  }

  const route = useMemo(() => parseRoute(effectivePath), [effectivePath]);

  if (isLoading) {
    return (
      <FullScreenMessage
        title="Loading dashboard"
        message="Checking your current session and security context…"
      />
    );
  }

  if (route.type === 'login') {
    return <LoginPage onLoginSuccess={() => navigate('/', true)} />;
  }

  if (!isAuthenticated) {
    return (
      <FullScreenMessage
        title="Redirecting"
        message="Your session is unavailable. Please sign in to continue."
      />
    );
  }

  const pageTitle = route.type === 'ticket-details' ? `Ticket #${route.ticketId}` : 'Dashboard';
  const pageSubtitle =
    route.type === 'ticket-details'
      ? 'View ticket details available to your current role.'
      : 'View and inspect current support tickets.';

  return (
    <AppLayout
      currentPath={route.type === 'ticket-details' ? '/' : effectivePath}
      onNavigate={navigate}
      pageTitle={pageTitle}
      pageSubtitle={pageSubtitle}
    >
      {route.type === 'ticket-details' ? (
        <TicketDetailsPage ticketId={route.ticketId} onBack={() => navigate('/')} />
      ) : (
        <DashboardPage onViewTicket={(id) => navigate(`/tickets/${id}`)} />
      )}
    </AppLayout>
  );
}

export default App;
