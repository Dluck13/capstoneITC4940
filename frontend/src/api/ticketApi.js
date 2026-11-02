import { apiRequest } from './httpClient';

const validateTicketId = (ticketId) => {
  if (!ticketId || Number.isNaN(Number(ticketId))) {
    throw new Error('A valid ticket id is required.');
  }
};

export const getTickets = () =>
  apiRequest('/api/tickets', {
    method: 'GET',
    includeCsrf: false,
  });

export const getTicketById = (ticketId) => {
  validateTicketId(ticketId);

  return apiRequest(`/api/tickets/${ticketId}`, {
    method: 'GET',
    includeCsrf: false,
  });
};

export const getCategories = () =>
  apiRequest('/api/categories', {
    method: 'GET',
    includeCsrf: false,
  });

export const createTicket = (request) =>
  apiRequest('/api/tickets', {
    method: 'POST',
    body: request,
  });

export const getTechnicians = () =>
  apiRequest('/api/technicians', {
    method: 'GET',
    includeCsrf: false,
  });

export const assignTicket = (ticketId, request) => {
  validateTicketId(ticketId);

  return apiRequest(`/api/tickets/${ticketId}/assign`, {
    method: 'PATCH',
    body: request,
  });
};

export const updateTicketStatus = (ticketId, request) => {
  validateTicketId(ticketId);

  return apiRequest(`/api/tickets/${ticketId}/status`, {
    method: 'PATCH',
    body: request,
  });
};

export const getTicketComments = (ticketId) => {
  validateTicketId(ticketId);

  return apiRequest(`/api/tickets/${ticketId}/comments`, {
    method: 'GET',
    includeCsrf: false,
  });
};

export const addTicketComment = (ticketId, request) => {
  validateTicketId(ticketId);

  return apiRequest(`/api/tickets/${ticketId}/comments`, {
    method: 'POST',
    body: request,
  });
};

export const resolveTicket = (ticketId, request) => {
  validateTicketId(ticketId);

  return apiRequest(`/api/tickets/${ticketId}/resolve`, {
    method: 'PATCH',
    body: request,
  });
};

export const closeTicket = (ticketId) => {
  validateTicketId(ticketId);

  return apiRequest(`/api/tickets/${ticketId}/close`, {
    method: 'PATCH',
  });
};
