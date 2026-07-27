import { useMutation } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface FeedbackEntry {
  id: number;
  rating: number;
  message: string;
  userName: string;
  userEmail: string;
  createdAt: string;
}

export function useSubmitFeedback() {
  return useMutation({
    mutationFn: (body: { rating: number; message: string }) =>
      apiRequest<FeedbackEntry>('/feedback', { method: 'POST', body }),
  });
}
