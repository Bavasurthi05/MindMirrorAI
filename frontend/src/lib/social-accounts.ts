import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface SocialAccountSummary {
  id: number;
  provider: string;
  externalAccountId: string;
  displayName?: string;
  profileImageUrl?: string;
  status: string;
  connectedAt?: string;
  lastSyncedAt?: string;
}

export interface ConnectSocialAccountPayload {
  provider: string;
  externalAccountId: string;
  displayName?: string;
  profileImageUrl?: string;
  scope?: string;
}

export interface SocialContentImportPayload {
  provider: string;
  content: string;
}

export interface JournalAnalysis {
  sentiment: string;
  sentimentScore: number;
  emotion: string;
  emotionScores: Record<string, number>;
  explanation: Array<{ token: string; weight: number }>;
  prediction: string;
  predictionConfidence: number;
  predictionProbabilities: Record<string, number>;
  reasons: Array<{ feature: string; weight: number; percentage: number }>;
  modelBackend: string;
}

export function useSocialAccounts() {
  return useQuery({
    queryKey: ['social-accounts'],
    queryFn: () => apiRequest<SocialAccountSummary[]>('/social-accounts'),
  });
}

export function useConnectSocialAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ConnectSocialAccountPayload) =>
      apiRequest<SocialAccountSummary>('/social-accounts', { method: 'POST', body: payload }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['social-accounts'] });
    },
  });
}

export function useDisconnectSocialAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (accountId: number) => apiRequest<void>(`/social-accounts/${accountId}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['social-accounts'] });
    },
  });
}

export function useImportSocialContent() {
  return useMutation({
    mutationFn: (payload: SocialContentImportPayload) =>
      apiRequest<JournalAnalysis>('/social-accounts/import', { method: 'POST', body: payload }),
  });
}
