import { useEffect, useMemo, useState } from 'react';
import { ApiError, setUnauthorizedHandler } from '../api/httpClient';
import {
  getCurrentUser,
  initializeCsrf,
  login as loginRequest,
  logout as logoutRequest,
  resetCsrf,
} from '../api/authApi';
import { normalizeUser } from './roleUtils';
import { AuthContext } from './authContextStore';

const toMessage = (error, fallback) => {
  if (error instanceof ApiError && error.message) {
    return error.message;
  }
  return fallback;
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [globalError, setGlobalError] = useState('');

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      setSessionExpired(true);
    });

    return () => setUnauthorizedHandler(null);
  }, []);

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      setIsLoading(true);
      setGlobalError('');

      try {
        await initializeCsrf();
        const me = await getCurrentUser();
        if (!active) return;

        setUser(normalizeUser(me));
        setSessionExpired(false);
      } catch (error) {
        if (!active) return;

        if (error instanceof ApiError && error.status === 401) {
          setUser(null);
          if (window.location.pathname !== '/login') {
            setSessionExpired(true);
          }
        } else {
          setGlobalError(toMessage(error, 'Unable to verify your session.'));
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    bootstrap();

    return () => {
      active = false;
    };
  }, []);

  const login = async ({ email, password }) => {
    setGlobalError('');

    try {
      await initializeCsrf();
      const profile = await loginRequest(email, password);
      const normalized = normalizeUser(profile);
      setUser(normalized);
      await initializeCsrf();
      setSessionExpired(false);
      return { success: true, user: normalized };
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        return { success: false, message: 'Invalid email or password.' };
      }

      return {
        success: false,
        message: toMessage(error, 'Unable to sign in right now. Please try again.'),
      };
    }
  };

  const logout = async () => {
    try {
      await logoutRequest();
    } catch (error) {
      if (!(error instanceof ApiError) || (error.status !== 401 && error.status !== 403)) {
        setGlobalError(toMessage(error, 'Unable to sign out cleanly.'));
      }
    } finally {
      setUser(null);
      resetCsrf();
      await initializeCsrf().catch(() => {
        // Best effort; frontend auth state is already cleared.
      });
    }
  };

  const value = useMemo(
    () => ({
      user,
      isLoading,
      sessionExpired,
      globalError,
      isAuthenticated: Boolean(user),
      login,
      logout,
      clearSessionExpired: () => setSessionExpired(false),
      clearGlobalError: () => setGlobalError(''),
    }),
    [globalError, isLoading, sessionExpired, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
