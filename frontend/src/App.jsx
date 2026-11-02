import { useCallback, useEffect, useMemo, useState } from 'react';
import './App.css';
import { useAuth } from './auth/useAuth';
import AppLayout from './components/AppLayout';
import AdminUserCreatePage from './pages/AdminUserCreatePage';
import CreateTicketPage from './pages/CreateTicketPage';
import DashboardPage from './pages/DashboardPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import TicketDetailsPage from './pages/TicketDetailsPage';

const NEW_TICKET_ROUTE = '/tickets/new';
const HOME_ROUTE = '/home';
const REGISTER_ROUTE = '/register';
const ADMIN_USERS_NEW_ROUTE = '/admin/users/new';
const TICKET_ROUTE = /^\/tickets\/(\d+)$/;

const isValidPath = (path) =>
  path === '/' ||
  path === HOME_ROUTE ||
  path === '/login' ||
  path === REGISTER_ROUTE ||
  path === NEW_TICKET_ROUTE ||
  path === ADMIN_USERS_NEW_ROUTE ||
  TICKET_ROUTE.test(path);

const sanitizePath = (path) => (isValidPath(path) ? path : '/');

const getPath = () => sanitizePath(window.location.pathname || '/');

const isPublicPath = (path) => path === '/' || path === HOME_ROUTE || path === '/login' || path === REGISTER_ROUTE;

const parseRoute = (path, isAuthenticated) => {
  if (path === '/login') {
    return { type: 'login' };
  }

  if (path === REGISTER_ROUTE) {
    return { type: 'register' };
  }

  if (path === HOME_ROUTE) {
    return { type: 'home' };
  }

  if (path === '/') {
    return { type: isAuthenticated ? 'dashboard' : 'home' };
  }

  if (path === NEW_TICKET_ROUTE) {
    return { type: 'create-ticket' };
  }

  if (path === ADMIN_USERS_NEW_ROUTE) {
    return { type: 'admin-users-new' };
  }

  const match = path.match(TICKET_ROUTE);
  if (match) {
    return {
      type: 'ticket-details',
      ticketId: Number(match[1]),
    };
  }

  return { type: isAuthenticated ? 'dashboard' : 'home' };
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
  const { isLoading, isAuthenticated, user } = useAuth();
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
  if (!isAuthenticated && !isPublicPath(effectivePath)) {
    effectivePath = '/login';
  } else if (isAuthenticated && (effectivePath === '/login' || effectivePath === REGISTER_ROUTE)) {
    effectivePath = '/';
  }

  if (!isLoading && window.location.pathname !== effectivePath) {
    window.history.replaceState({}, '', effectivePath);
  }

  const route = useMemo(() => parseRoute(effectivePath, isAuthenticated), [effectivePath, isAuthenticated]);

  if (isLoading) {
    return (
      <FullScreenMessage
        title="Loading dashboard"
        message="Checking your current session and security context…"
      />
    );
  }

  if (route.type === 'home') {
    return (
      <HomePage
        isAuthenticated={isAuthenticated}
        onGoToRegister={() => navigate(REGISTER_ROUTE)}
        onGoToLogin={() => navigate('/login')}
        onGoToDashboard={() => navigate('/')}
      />
    );
  }

  if (route.type === 'login') {
    return (
      <LoginPage
        onLoginSuccess={() => navigate('/', true)}
        onCreateAccount={() => navigate(REGISTER_ROUTE)}
        onBackHome={() => navigate(HOME_ROUTE)}
      />
    );
  }

  if (route.type === 'register') {
    return (
      <RegisterPage
        onBackToLogin={() => navigate('/login', true)}
        onRegistered={() => navigate('/login', true)}
      />
    );
  }

  if (!isAuthenticated) {
    return (
      <FullScreenMessage
        title="Redirecting"
        message="Your session is unavailable. Please sign in to continue."
      />
    );
  }

  const pageTitle =
    route.type === 'ticket-details'
      ? `Ticket #${route.ticketId}`
      : route.type === 'admin-users-new'
        ? 'Account Management'
      : route.type === 'create-ticket'
        ? 'Create Ticket'
        : 'TicketCoop Dashboard';

  const pageSubtitle =
    route.type === 'ticket-details'
      ? 'View ticket details available to your current role.'
      : route.type === 'admin-users-new'
        ? 'Create TECHNICIAN and ADMIN accounts as an administrator.'
      : route.type === 'create-ticket'
        ? 'Submit a new support request for IT operations.'
        : 'IT Support Operations Dashboard';

  const activeNavPath =
    route.type === 'create-ticket'
      ? NEW_TICKET_ROUTE
      : route.type === 'admin-users-new'
        ? ADMIN_USERS_NEW_ROUTE
        : '/';

  return (
    <AppLayout
      currentPath={activeNavPath}
      onNavigate={navigate}
      pageTitle={pageTitle}
      pageSubtitle={pageSubtitle}
    >
      {route.type === 'admin-users-new' ? (
        user?.role === 'ADMIN' ? (
          <AdminUserCreatePage />
        ) : (
          <section className="card">
            <h2>Access denied</h2>
            <p className="info-text">Only administrators can access account management.</p>
          </section>
        )
      ) : route.type === 'ticket-details' ? (
        <TicketDetailsPage ticketId={route.ticketId} onBack={() => navigate('/')} />
      ) : route.type === 'create-ticket' ? (
        <CreateTicketPage onCancel={() => navigate('/')} onCreated={(ticketId) => navigate(`/tickets/${ticketId}`)} />
      ) : (
        <DashboardPage
          onCreateTicket={() => navigate(NEW_TICKET_ROUTE)}
          onViewTicket={(id) => navigate(`/tickets/${id}`)}
        />
      )}
    </AppLayout>
  );
}

export default App;
