import { useQuery } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface TriggerStrength {
  title: string;
  strength: string;
}

export interface ReportSummary {
  overallWellness: number;
  stressTrend: string;
  recoveryScore: number;
  journalCount: number;
  averageMood: number;
  latestSeverity: string;
  summaryText: string;
  topTriggers: TriggerStrength[];
  recommendations: string[];
  predictionSummary: string[];
}

export function useReportSummary() {
  return useQuery({
    queryKey: ['report', 'summary'],
    queryFn: () => apiRequest<ReportSummary>('/reports/summary'),
  });
}
