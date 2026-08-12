import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface UserPreferences {
  timezone: string;
  reminderEnabled: boolean;
  /** Local 24h "HH:mm". */
  reminderTime: string;
  focusAreas: string[];
  /**
   * Opt-in for using this account's text as model training data. Default off and revocable;
   * the app is fully functional without it.
   */
  trainingConsent: boolean;
  trainingConsentAt: string | null;
  onboardingCompleted: boolean;
  onboardingStep: number;
}

export type UpdatePreferencesPayload = Partial<{
  timezone: string;
  reminderEnabled: boolean;
  reminderTime: string;
  focusAreas: string[];
  trainingConsent: boolean;
  onboardingStep: number;
  onboardingCompleted: boolean;
}>;

export const FOCUS_AREAS = [
  'Sleep',
  'Work stress',
  'Anxiety',
  'Low mood',
  'Relationships',
  'Energy',
  'Focus',
  'Routine',
] as const;

export function usePreferences() {
  return useQuery({
    queryKey: ['me', 'preferences'],
    queryFn: () => apiRequest<UserPreferences>('/me/preferences'),
  });
}

export function useUpdatePreferences() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdatePreferencesPayload) =>
      apiRequest<UserPreferences>('/me/preferences', { method: 'PATCH', body: payload }),
    onSuccess: (data) => {
      queryClient.setQueryData(['me', 'preferences'], data);
      // Daily buckets are timezone-dependent, so a zone change invalidates the dashboard.
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
    },
  });
}

/** The browser's IANA zone, used to pre-fill onboarding. */
export function detectTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}
