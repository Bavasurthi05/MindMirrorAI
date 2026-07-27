import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface TriggerEntry {
  id: number;
  category: string;
  intensity: number;
  note: string | null;
  occurredAt: string;
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

export function useLogTrigger() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: TriggerInput) => apiRequest<TriggerEntry>('/triggers', { method: 'POST', body: input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: TRIGGER_KEY });
    },
  });
}
