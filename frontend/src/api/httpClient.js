const DEFAULT_API_BASE_URL = 'http://localhost:8080';

const normalizeBaseUrl = (url) => {
  if (!url) return DEFAULT_API_BASE_URL;
  return url.endsWith('/') ? url.slice(0, -1) : url;
};

export const API_BASE_URL = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL);

let csrfHeaderName = 'X-CSRF-TOKEN';
let csrfToken = null;
let unauthorizedHandler = null;

export class ApiError extends Error {
  constructor(message, { status = 0, data = null, network = false } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
    this.network = network;
  }
}

const isStateChangingMethod = (method) => {
  const normalized = method.toUpperCase();
  return normalized !== 'GET' && normalized !== 'HEAD' && normalized !== 'OPTIONS';
};

const parseResponseBody = async (response) => {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  const text = await response.text();

  if (!text) {
    return null;
  }

  if (contentType.includes('application/json')) {
    try {
      return JSON.parse(text);
    } catch {
      return null;
    }
  }

  return text;
};

const deriveErrorMessage = (status, data, fallback) => {
  if (data && typeof data === 'object' && typeof data.message === 'string') {
    return data.message;
  }
  if (typeof data === 'string' && data.trim()) {
    return data;
  }
  if (fallback?.trim()) {
    return fallback;
  }
  return status === 0
    ? 'Unable to reach the server. Please check your connection.'
    : `Request failed with status ${status}.`;
};

const buildUrl = (path) => {
  if (!path.startsWith('/')) {
    throw new Error(`API path must start with '/': ${path}`);
  }
  return `${API_BASE_URL}${path}`;
};

export const setUnauthorizedHandler = (handler) => {
  unauthorizedHandler = typeof handler === 'function' ? handler : null;
};

export const fetchCsrfToken = async () => {
  let response;
  try {
    response = await fetch(buildUrl('/api/auth/csrf'), {
      method: 'GET',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
      },
    });
  } catch {
    throw new ApiError('Unable to reach the server. Please check your connection.', {
      status: 0,
      network: true,
    });
  }

  const data = await parseResponseBody(response);

  if (!response.ok) {
    throw new ApiError(deriveErrorMessage(response.status, data, response.statusText), {
      status: response.status,
      data,
    });
  }

  if (!data || typeof data !== 'object' || !data.headerName || !data.token) {
    throw new ApiError('Invalid CSRF response from server.', {
      status: response.status,
      data,
    });
  }

  csrfHeaderName = data.headerName;
  csrfToken = data.token;
  return { headerName: csrfHeaderName, token: csrfToken };
};

export const clearCsrfToken = () => {
  csrfToken = null;
};

export const apiRequest = async (path, options = {}) => {
  const {
    method = 'GET',
    body,
    headers = {},
    suppressUnauthorizedHandler = false,
    includeCsrf = true,
    signal,
  } = options;

  const requestHeaders = {
    Accept: 'application/json',
    ...headers,
  };

  if (body !== undefined) {
    requestHeaders['Content-Type'] = requestHeaders['Content-Type'] || 'application/json';
  }

  if (includeCsrf && isStateChangingMethod(method)) {
    if (!csrfToken) {
      await fetchCsrfToken();
    }
    requestHeaders[csrfHeaderName] = csrfToken;
  }

  const requestInit = {
    method,
    credentials: 'include',
    headers: requestHeaders,
    signal,
  };

  if (body !== undefined) {
    requestInit.body = typeof body === 'string' ? body : JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(buildUrl(path), requestInit);
  } catch {
    throw new ApiError('Unable to reach the server. Please check your connection.', {
      status: 0,
      network: true,
    });
  }

  const data = await parseResponseBody(response);

  if (!response.ok) {
    if (response.status === 401 && !suppressUnauthorizedHandler && unauthorizedHandler) {
      unauthorizedHandler();
    }

    throw new ApiError(deriveErrorMessage(response.status, data, response.statusText), {
      status: response.status,
      data,
    });
  }

  return data;
};