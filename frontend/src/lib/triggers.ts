import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface TriggerEntry {
  id: number;
  category: string;
  intensity: number;
  note: string | null;
  occurredAt: string;
  /** USER_LOGGED when entered by hand, DERIVED when detected in the user's writing. */
  source: string;
  confirmation: 'NOT_REQUIRED' | 'PENDING' | 'CONFIRMED' | 'DISMISSED';
  analysisResultId: number | null;
}

export interface CategoryStat {
  category: string;
  count: number;
  averageIntensity: number;
}

export interface TriggerAnalytics {
  totalCount: number;
  averageIntensity: number;
  categories: CategoryStat[];
}

export interface TriggerInput {
  category: string;
  intensity: number;
  note?: string;
}

const TRIGGER_KEY = ['triggers'] as const;

export function useTriggers() {
  return useQuery({
    queryKey: TRIGGER_KEY,
    queryFn: () => apiRequest<TriggerEntry[]>('/triggers'),
  });
}

export function useTriggerAnalytics() {
  return useQuery({
    queryKey: [...TRIGGER_KEY, 'analytics'],
    queryFn: () => apiRequest<TriggerAnalytics>('/triggers/analytics'),
  });
}

/** Auto-detected triggers awaiting the user's verdict. */
export function usePendingTriggers() {
  return useQuery({
    queryKey: [...TRIGGER_KEY, 'pending'],
    queryFn: () => apiRequest<TriggerEntry[]>('/triggers/pending'),
  });
}

export function useConfirmTrigger() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, intensity }: { id: number; intensity?: number }) =>
      apiRequest<TriggerEntry>(
        `/triggers/${id}/confirm${intensity ? `?intensity=${intensity}` : ''}`,
        { method: 'PATCH' },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRIGGER_KEY });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
    },
  });
}

/** Rejecting a detection removes it from analytics and records that we got it wrong. */
export function useDismissTrigger() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiRequest<TriggerEntry>(`/triggers/${id}/dismiss`, { method: 'PATCH' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRIGGER_KEY });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
    },
  });
}

export function useLogTrigger() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: TriggerInput) => apiRequest<TriggerEntry>('/triggers', { method: 'POST', body: input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRIGGER_KEY });
    },
  });
}
