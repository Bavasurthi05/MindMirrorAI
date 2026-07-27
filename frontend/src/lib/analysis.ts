import { useMutation, useQuery } from '@tanstack/react-query';
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

export interface JournalAnalysis {
  sentiment: string;
  sentimentScore: number;
  emotion: string;
  emotionScores: Record<string, number>;
  explanation: TokenContribution[];
  prediction: string;
  predictionConfidence: number;
  predictionProbabilities: Record<string, number>;
  reasons: FeatureReason[];
  modelBackend: string;
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
  labels: string[];
  trainSize: number;
  testSize: number;
  models: Record<string, ModelInfo>;
}

export function useMoodPrediction() {
  return useQuery({
    queryKey: ['analysis', 'mood-prediction'],
    queryFn: () => apiRequest<MoodPrediction>('/analysis/mood-prediction'),
  });
}

export function useAnalyzeJournal() {
  return useMutation({
    mutationFn: (text: string) => apiRequest<JournalAnalysis>('/analysis/journal', { method: 'POST', body: { text } }),
  });
}

export function useAnalyzeSocial() {
  return useMutation({
    mutationFn: (text: string) => apiRequest<JournalAnalysis>('/analysis/social', { method: 'POST', body: { text } }),
  });
}

export function useModelMetrics() {
  return useQuery({
    queryKey: ['analysis', 'model-metrics'],
    queryFn: () => apiRequest<ModelMetrics>('/analysis/model-metrics'),
  });
}
