import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface TokenContribution {
  token: string;
  weight: number;
}

export interface FeatureReason {
  feature: string;
  weight: number;
  percentage: number;
}

export interface DetectedTrigger {
  category: string;
  matchedTerms: string[];
  intensity: number;
}

/**
 * A stored analysis. Results are persisted server-side, so they survive a refresh and can be
 * re-read later rather than existing only in the response of the request that created them.
 */
export interface AnalysisResult {
  id: number;
  sourceType: 'JOURNAL' | 'SOCIAL' | 'QUESTIONNAIRE' | 'CHECKIN' | 'ADHOC';
  sourceId: number | null;
  status: 'PENDING' | 'OK' | 'FAILED';
  errorMessage: string | null;
  sentiment: string | null;
  sentimentScore: number | null;
  dominantEmotion: string | null;
  emotionScores: Record<string, number>;
  prediction: string | null;
  predictionConfidence: number | null;
  predictionProbabilities: Record<string, number>;
  reasons: FeatureReason[];
  explanation: TokenContribution[];
  triggers: DetectedTrigger[];
  modelBackend: string | null;
  modelVersion: string | null;
  analyzedAt: string | null;
}

export interface MoodPrediction {
  predictedScore: number;
  trend: string;
  confidence: number;
  rationale: string;
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
  version: string;
  labels: string[];
  emotionLabels: string[];
  trainSize: number;
  testSize: number;
  datasetProfile: Record<string, unknown>;
  models: Record<string, ModelInfo>;
}

export interface MlHealth {
  reachable: boolean;
  status: string;
  modelVersion: string;
  modelAvailable: boolean;
  detail: string | null;
}

export function useMoodPrediction() {
  return useQuery({
    queryKey: ['analysis', 'mood-prediction'],
    queryFn: () => apiRequest<MoodPrediction>('/analysis/mood-prediction'),
  });
}

export function useAnalyzeJournal() {
  return useMutation({
    mutationFn: (text: string) =>
      apiRequest<AnalysisResult>('/analysis/journal', { method: 'POST', body: { text } }),
  });
}

export function useAnalyzeSocial() {
  return useMutation({
    mutationFn: (text: string) =>
      apiRequest<AnalysisResult>('/analysis/social', { method: 'POST', body: { text } }),
  });
}

/** Recent completed analyses, newest first. */
export function useRecentAnalyses(limit = 10) {
  return useQuery({
    queryKey: ['analysis', 'results', limit],
    queryFn: () => apiRequest<AnalysisResult[]>(`/analysis/results?limit=${limit}`),
  });
}

/**
 * The analysis for a journal entry. Journal analysis runs in the background, so this polls
 * briefly while the result is still pending and stops once it arrives.
 */
export function useAnalysisForEntry(journalEntryId: number | null) {
  return useQuery({
    queryKey: ['analysis', 'journal', journalEntryId],
    enabled: journalEntryId !== null,
    queryFn: () => apiRequest<AnalysisResult | null>(`/analysis/journal/${journalEntryId}`),
    refetchInterval: (query) => {
      const result = query.state.data;
      return !result || result.status === 'PENDING' ? 2000 : false;
    },
  });
}

export function useModelMetrics() {
  return useQuery({
    queryKey: ['analysis', 'model-metrics'],
    queryFn: () => apiRequest<ModelMetrics>('/analysis/model-metrics'),
  });
}

export function useMlHealth() {
  return useQuery({
    queryKey: ['analysis', 'health'],
    queryFn: () => apiRequest<MlHealth>('/analysis/health'),
  });
}

// --- Prediction feedback ------------------------------------------------------------

export type FeedbackAgreement = 'AGREE' | 'DISAGREE' | 'PARTIAL';

export interface PredictionFeedback {
  id: number;
  analysisResultId: number;
  predictedLabel: string | null;
  correctedLabel: string | null;
  agreement: FeedbackAgreement;
  comment: string | null;
  createdAt: string;
}

export interface PredictionFeedbackPayload {
  agreement: FeedbackAgreement;
  /** Required when disagreeing. */
  correctedLabel?: string;
  comment?: string;
}

/** The states the model can predict — the only valid corrections. */
export const PREDICTION_LABELS = ['normal', 'stress', 'anxiety', 'depression'] as const;

export function usePredictionFeedback(analysisId: number | null) {
  return useQuery({
    queryKey: ['analysis', 'feedback', analysisId],
    enabled: analysisId !== null,
    queryFn: () => apiRequest<PredictionFeedback | null>(`/analysis/results/${analysisId}/feedback`),
  });
}

export function useSubmitPredictionFeedback(analysisId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: PredictionFeedbackPayload) =>
      apiRequest<PredictionFeedback>(`/analysis/results/${analysisId}/feedback`, {
        method: 'POST',
        body: payload,
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(['analysis', 'feedback', analysisId], data);
    },
  });
}
