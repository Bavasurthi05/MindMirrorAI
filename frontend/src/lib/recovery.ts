import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface RecoveryAction {
  id: number;
  title: string;
  focus: string | null;
  duration: string | null;
  description: string | null;
  completed: boolean;
}

const RECOVERY_KEY = ['recovery'] as const;

export function useRecoveryPlan() {
  return useQuery({
    queryKey: RECOVERY_KEY,
    queryFn: () => apiRequest<RecoveryAction[]>('/recovery'),
  });
}

export function useToggleRecoveryAction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiRequest<RecoveryAction>(`/recovery/${id}/toggle`, { method: 'PATCH' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: RECOVERY_KEY });
    },
  });
}
