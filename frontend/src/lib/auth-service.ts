import { apiRequest } from './api';

export function forgotPassword(email: string): Promise<void> {
  return apiRequest<void>('/auth/forgot-password', {
    method: 'POST',
    body: { email },
    auth: false,
  });
}

export function resetPassword(token: string, newPassword: string): Promise<void> {
  return apiRequest<void>('/auth/reset-password', {
    method: 'POST',
    body: { token, newPassword },
    auth: false,
  });
}

export function verifyEmail(token: string): Promise<void> {
  return apiRequest<void>(`/auth/verify-email?token=${encodeURIComponent(token)}`, {
    method: 'GET',
    auth: false,
  });
}
