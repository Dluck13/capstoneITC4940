import { apiRequest } from './httpClient';

export const getTickets = () =>
  apiRequest('/api/tickets', {
    method: 'GET',
    includeCsrf: false,
  });

export const getTicketById = (id) => {
  if (!id || Number.isNaN(Number(id))) {
    throw new Error('A valid ticket id is required.');
  }

  return apiRequest(`/api/tickets/${id}`, {
    method: 'GET',
    includeCsrf: false,
  });
};