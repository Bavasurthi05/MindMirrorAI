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

export interface TriggerHeatCell {
  /** MONDAY … SUNDAY. */
  day: string;
  /** Morning · Midday · Evening · Night. */
  slot: string;
  /** Null when nothing was logged in that cell — not the same as a calm zero. */
  averageIntensity: number | null;
  count: number;
}

export interface WeekdayStat {
  day: string;
  averageIntensity: number | null;
  count: number;
}

export interface TriggerAnalytics {
  totalCount: number;
  averageIntensity: number;
  categories: CategoryStat[];
  heatmap: TriggerHeatCell[];
  weekdayIntensity: WeekdayStat[];
}

export const TRIGGER_SLOTS = ['Morning', 'Midday', 'Evening', 'Night'] as const;
export const TRIGGER_DAYS = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
] as const;
export const TRIGGER_DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

/** Colour for a trigger-intensity cell. Null (no data) stays neutral, never "calm". */
export function triggerHeatClass(intensity: number | null): string {
  if (intensity === null || intensity === undefined) return 'bg-slate-100 dark:bg-slate-800';
  if (intensity >= 7.5) return 'bg-rose-500';
  if (intensity >= 5.5) return 'bg-orange-400';
  if (intensity >= 3.5) return 'bg-amber-400';
  if (intensity >= 1.5) return 'bg-sky-400';
  return 'bg-emerald-400';
}

/** Look up one cell of the 7x4 grid. */
export function findHeatCell(
  cells: TriggerHeatCell[] | undefined,
  day: string,
  slot: string,
): TriggerHeatCell | undefined {
  return cells?.find((cell) => cell.day === day && cell.slot === slot);
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
