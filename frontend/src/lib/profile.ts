import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface Profile {
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  role: string;
  emailVerified: boolean;
  memberSince: string;
  journalStreak: number;
  journalCount: number;
  moodCount: number;
  goalsCompleted: number;
  goalsTotal: number;
}

export interface UpdateProfilePayload {
  firstName: string;
  lastName: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export function useProfile() {
  return useQuery({
    queryKey: ['me', 'profile'],
    queryFn: () => apiRequest<Profile>('/me/profile'),
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateProfilePayload) => apiRequest<Profile>('/me/profile', {
      method: 'PATCH',
      body: payload,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['me', 'profile'] });
    },
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (payload: ChangePasswordPayload) => apiRequest<void>('/me/password', {
      method: 'POST',
      body: payload,
    }),
  });
}
