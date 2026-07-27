import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface Goal {
  id: number;
  title: string;
  target: number;
  progress: number;
  period: string;
  completed: boolean;
}

export function useGoals() {
  return useQuery({
    queryKey: ['goals'],
    queryFn: () => apiRequest<Goal[]>('/goals'),
  });
}

export function useCreateGoal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { title: string; target: number; period?: string }) =>
      apiRequest<Goal>('/goals', { method: 'POST', body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['goals'] }),
  });
}

export function useIncrementGoal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiRequest<Goal>(`/goals/${id}/progress`, { method: 'POST' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['goals'] }),
  });
}

export function useDeleteGoal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiRequest<void>(`/goals/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['goals'] }),
  });
}
