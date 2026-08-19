import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface GrowthPoint {
  /** e.g. "Mar 2026". */
  label: string;
  newUsers: number;
  cumulativeUsers: number;
}

export interface CategoryCount {
  category: string;
  count: number;
}

export interface AdminOverview {
  totalUsers: number;
  verifiedUsers: number;
  totalJournalEntries: number;
  totalMoodEntries: number;
  totalAssessments: number;
  totalTriggers: number;
  totalRecoveryActions: number;
  /** Real sign-ups for the trailing 6 months, oldest first. */
  userGrowth: GrowthPoint[];
  /** Trigger categories across all users, most common first. */
  triggerDistribution: CategoryCount[];
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
  emotionLabels?: string[];
  trainSize: number;
  testSize: number;
  datasetProfile?: {
    name?: string;
    source?: string;
    sourceDatasetSize?: number;
    trainingSamplesUsed?: number;
  };
  models: Record<string, ModelInfo>;
}

export function useAdminModelMetrics() {
  return useQuery({
    queryKey: ['admin', 'model-metrics'],
    queryFn: () => apiRequest<ModelMetrics>('/admin/model-metrics'),
  });
}

// --- Prediction feedback (training-label readiness) ---------------------------------

export interface FeedbackStats {
  total: number;
  agree: number;
  disagree: number;
  partial: number;
  /** Null before any feedback exists, rather than a misleading 0%. */
  agreementRatePercent: number | null;
  correctedLabelCounts: Record<string, number>;
}

export interface AdminPredictionFeedback {
  id: number;
  analysisResultId: number;
  predictedLabel: string | null;
  correctedLabel: string | null;
  agreement: 'AGREE' | 'DISAGREE' | 'PARTIAL';
  comment: string | null;
  createdAt: string;
}

/** Thresholds the retraining gate will require — surfaced so readiness is visible early. */
export const MIN_EXAMPLES_TO_RETRAIN = 200;
export const MIN_EXAMPLES_PER_CLASS = 30;

export function useFeedbackStats() {
  return useQuery({
    queryKey: ['admin', 'prediction-feedback', 'stats'],
    queryFn: () => apiRequest<FeedbackStats>('/admin/prediction-feedback/stats'),
  });
}

export function useAdminPredictionFeedback(limit = 50) {
  return useQuery({
    queryKey: ['admin', 'prediction-feedback', limit],
    queryFn: () => apiRequest<AdminPredictionFeedback[]>(`/admin/prediction-feedback?limit=${limit}`),
  });
}

/** Whether enough labels, spread across enough classes, have accumulated to retrain on. */
export function retrainingReadiness(stats: FeedbackStats): {
  ready: boolean;
  reasons: string[];
} {
  const reasons: string[] = [];
  const labelled = Object.values(stats.correctedLabelCounts).reduce((sum, count) => sum + count, 0);
  if (stats.total < MIN_EXAMPLES_TO_RETRAIN) {
    reasons.push(`${stats.total}/${MIN_EXAMPLES_TO_RETRAIN} total responses`);
  }
  const thinClasses = Object.entries(stats.correctedLabelCounts)
    .filter(([, count]) => count < MIN_EXAMPLES_PER_CLASS)
    .map(([label]) => label);
  if (labelled === 0) {
    reasons.push('no corrected labels yet');
  } else if (thinClasses.length > 0) {
    reasons.push(`under ${MIN_EXAMPLES_PER_CLASS} for: ${thinClasses.join(", ")}`);
  }
  return { ready: reasons.length === 0, reasons };
}

// --- Retraining & model registry ----------------------------------------------------

export interface GateCheck {
  name: string;
  passed: boolean;
  detail: string;
}

export interface TrainingRun {
  id: number;
  jobId: string | null;
  version: string | null;
  status: string;
  triggeredBy: string;
  corpusTotal: number;
  corpusUserExamples: number;
  accuracy: number | null;
  f1Macro: number | null;
  gatePassed: boolean;
  gateChecks: GateCheck[];
  gateFailures: string[];
  promoted: boolean;
  promotedAt: string | null;
  promotedBy: string | null;
  /** True when an admin knowingly deployed a version that failed the gate. */
  forced: boolean;
  message: string | null;
  createdAt: string;
}

export interface ModelVersion {
  version: string;
  createdAt: string | null;
  promoted: boolean;
  promotedAt: string | null;
  metrics: Record<string, unknown>;
  corpus: Record<string, unknown>;
  gate: { passed: boolean; checks: GateCheck[]; failed: string[] };
}

export interface ModelVersions {
  active: string | null;
  versions: ModelVersion[];
}

export interface TrainingDataSummary {
  exportableExamples: number;
  labelCounts: Record<string, number>;
  consentingUsers: number;
}

export function useTrainingDataSummary() {
  return useQuery({
    queryKey: ['admin', 'training-data', 'summary'],
    queryFn: () => apiRequest<TrainingDataSummary>('/admin/training-data/summary'),
  });
}

export function useModelVersions() {
  return useQuery({
    queryKey: ['admin', 'models', 'versions'],
    queryFn: () => apiRequest<ModelVersions>('/admin/models/versions'),
  });
}

export function useTrainingRuns(limit = 20) {
  return useQuery({
    queryKey: ['admin', 'models', 'runs', limit],
    queryFn: () => apiRequest<TrainingRun[]>(`/admin/models/runs?limit=${limit}`),
  });
}

/** Polls a run until it finishes; training takes minutes, not milliseconds. */
export function useTrainingRunStatus(jobId: string | null) {
  return useQuery({
    queryKey: ['admin', 'models', 'run', jobId],
    enabled: jobId !== null,
    queryFn: () => apiRequest<TrainingRun>(`/admin/models/runs/${jobId}`),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'COMPLETED' || status === 'FAILED' ? false : 4000;
    },
  });
}

export function useStartRetrain() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiRequest<TrainingRun>('/admin/models/retrain', { method: 'POST' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'models'] }),
  });
}

export function usePromoteModel() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ version, force }: { version: string; force?: boolean }) =>
      apiRequest<{ active: string; reloaded: boolean; gatePassed: boolean; forced: boolean }>(
        `/admin/models/promote?version=${encodeURIComponent(version)}&force=${force ? 'true' : 'false'}`,
        { method: 'POST' },
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'models'] }),
  });
}

export function useRollbackModel() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (version: string) =>
      apiRequest<{ active: string; reloaded: boolean }>(
        `/admin/models/rollback?version=${encodeURIComponent(version)}`,
        { method: 'POST' },
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'models'] }),
  });
}
