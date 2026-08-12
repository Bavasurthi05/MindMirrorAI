import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';
import type { MoodEntry } from './mood';

export interface CheckInStatus {
  checkedInToday: boolean;
  /** Today in the user's own timezone, not the server's. */
  localDate: string;
  timezone: string;
  lastCheckInAt: string | null;
  lastMoodScore: number | null;
}

export interface CheckInPayload {
  moodScore: number;
  moodLabel?: string;
  note?: string;
}

/** The five faces, mapped onto the 0-100 mood scale the rest of the app uses. */
export const MOOD_FACES = [
  { score: 10, emoji: '😞', label: 'Rough' },
  { score: 30, emoji: '😕', label: 'Low' },
  { score: 50, emoji: '😐', label: 'Okay' },
  { score: 75, emoji: '🙂', label: 'Good' },
  { score: 95, emoji: '😄', label: 'Great' },
] as const;

export function useCheckInStatus() {
  return useQuery({
    queryKey: ['checkin', 'status'],
    queryFn: () => apiRequest<CheckInStatus>('/checkin'),
  });
}

export function useSubmitCheckIn() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CheckInPayload) =>
      apiRequest<MoodEntry>('/checkin', { method: 'POST', body: payload }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['checkin'] });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
      queryClient.invalidateQueries({ queryKey: ['mood'] });
    },
  });
}

/** Nearest face for a stored score, so a saved check-in shows the option the user picked. */
export function faceForScore(score: number | null): (typeof MOOD_FACES)[number] | null {
  if (score === null || score === undefined) {
    return null;
  }
  return MOOD_FACES.reduce((closest, face) =>
    Math.abs(face.score - score) < Math.abs(closest.score - score) ? face : closest,
  );
}
