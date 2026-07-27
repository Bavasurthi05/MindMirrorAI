import { useQuery } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface Profile {
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

export function useProfile() {
  return useQuery({
    queryKey: ['me', 'profile'],
    queryFn: () => apiRequest<Profile>('/me/profile'),
  });
}
