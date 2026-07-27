import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface AdminOverview {
  totalUsers: number;
  verifiedUsers: number;
  totalJournalEntries: number;
  totalMoodEntries: number;
  totalAssessments: number;
  totalTriggers: number;
  totalRecoveryActions: number;
}

export function useAdminOverview() {
  return useQuery({
    queryKey: ['admin', 'overview'],
    queryFn: () => apiRequest<AdminOverview>('/admin/overview'),
  });
}

export interface AdminUser {
  id: number;
  fullName: string;
  email: string;
  role: string;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
}

export function useAdminUsers() {
  return useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => apiRequest<AdminUser[]>('/admin/users'),
  });
}

export function useSetUserEnabled() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      apiRequest<AdminUser>(`/admin/users/${id}/enabled?enabled=${enabled}`, { method: 'PATCH' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });
}

export interface AdminFeedback {
  id: number;
  rating: number;
  message: string;
  userName: string;
  userEmail: string;
  createdAt: string;
}

export function useAdminFeedback() {
  return useQuery({
    queryKey: ['admin', 'feedback'],
    queryFn: () => apiRequest<AdminFeedback[]>('/admin/feedback'),
  });
}

export interface ModelInfo {
  name: string;
  accuracy: number;
  f1Macro: number;
  deployed: boolean;
}

export interface ModelMetrics {
  available: boolean;
  backend: string;
  labels: string[];
  trainSize: number;
  testSize: number;
  models: Record<string, ModelInfo>;
}

export function useAdminModelMetrics() {
  return useQuery({
    queryKey: ['admin', 'model-metrics'],
    queryFn: () => apiRequest<ModelMetrics>('/admin/model-metrics'),
  });
}
