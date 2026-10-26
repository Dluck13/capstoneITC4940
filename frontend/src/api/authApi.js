import { apiRequest, clearCsrfToken, fetchCsrfToken } from './httpClient';

export const initializeCsrf = () => fetchCsrfToken();

export const getCurrentUser = () =>
  apiRequest('/api/auth/me', {
    method: 'GET',
    suppressUnauthorizedHandler: true,
    includeCsrf: false,
  });

export const login = (email, password) =>
  apiRequest('/api/auth/login', {
    method: 'POST',
    body: { email, password },
    suppressUnauthorizedHandler: true,
  });

export const logout = () =>
  apiRequest('/api/auth/logout', {
    method: 'POST',
    includeCsrf: true,
    suppressUnauthorizedHandler: true,
  });

export const resetCsrf = () => clearCsrfToken();