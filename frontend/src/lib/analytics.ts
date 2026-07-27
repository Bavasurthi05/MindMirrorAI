import { useQuery } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface Metric {
  label: string;
  value: number;
}

export interface SeriesPoint {
  date: string;
  score: number;
}

export interface HeatCell {
  date: string;
  score: number | null;
}

export interface EmotionPoint {
  date: string;
  label: string;
  score: number;
}

export interface AnalyticsOverview {
  overallWellness: number;
  radar: Metric[];
  heatmap: HeatCell[];
  emotionTimeline: EmotionPoint[];
  weeklyTrend: Metric[];
  moodSeries: SeriesPoint[];
  emotionDistribution: Metric[];
  triggerDistribution: Metric[];
}

export function useAnalyticsOverview() {
  return useQuery({
    queryKey: ['analytics', 'overview'],
    queryFn: () => apiRequest<AnalyticsOverview>('/analytics/overview'),
  });
}

export interface WeeklyInsights {
  highlights: string[];
  focusArea: string;
  wellbeingIndex: number;
}

export function useWeeklyInsights() {
  return useQuery({
    queryKey: ['analytics', 'weekly-insights'],
    queryFn: () => apiRequest<WeeklyInsights>('/analytics/weekly-insights'),
  });
}
