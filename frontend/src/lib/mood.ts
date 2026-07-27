import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface MoodEntry {
  id: number;
  moodScore: number;
  moodLabel: string | null;
  note: string | null;
  recordedAt: string;
}

export interface MoodEntryInput {
  moodScore: number;
  moodLabel?: string;
  note?: string;
}

const MOOD_KEY = ['mood'] as const;

export function useRecentMoods(days = 30) {
  return useQuery({
    queryKey: [...MOOD_KEY, days],
    queryFn: () => apiRequest<MoodEntry[]>(`/mood?days=${days}`),
  });
}

export function useLogMood() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: MoodEntryInput) => apiRequest<MoodEntry>('/mood', { method: 'POST', body: input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: MOOD_KEY });
    },
  });
}
