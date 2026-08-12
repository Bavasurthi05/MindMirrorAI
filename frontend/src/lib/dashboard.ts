import { useQuery } from '@tanstack/react-query';
import { apiRequest } from './api';

/**
 * Nullable fields are meaningful: null means "not enough data to say".
 * Never substitute a default — render an empty state instead.
 */
export interface DashboardSummary {
  stress: string | null;
  energy: string | null;
  reflection: string | null;
}

export interface MoodDay {
  date: string;
  label: string;
  score: number | null;
}

export interface RecentAnalysis {
  id: number;
  date: string | null;
  headline: string;
  detail: string;
  prediction: string | null;
  sentiment: string | null;
}

export interface LabelledValue {
  label: string;
  value: string;
}

export interface DashboardRecommendation {
  id: number;
  title: string;
  detail: string | null;
}

export interface ProgressItem {
  label: string;
  progress: number | null;
}

export interface DataCompleteness {
  hasJournal: boolean;
  hasMood: boolean;
  hasAssessment: boolean;
  hasTriggers: boolean;
  hasAnalysis: boolean;
  daysOfData: number;
  pendingAnalyses: number;
  onboardingCompleted: boolean;
  /** Null when never assessed. */
  daysSinceAssessment: number | null;
}

export interface DashboardCheckIn {
  checkedInToday: boolean;
  streak: number;
  todaysScore: number | null;
}

export interface Dashboard {
  wellnessScore: number | null;
  wellnessDelta: number | null;
  wellnessUpdatedAt: string | null;
  /** Band and one-line reading of the composite score (see lib/mindset.ts). */
  mindsetBand: string | null;
  mindsetInsight: string | null;
  headline: string;
  summary: DashboardSummary;
  moodWeek: MoodDay[];
  recentAnalyses: RecentAnalysis[];
  triggerSummary: LabelledValue[];
  recommendations: DashboardRecommendation[];
  progressItems: ProgressItem[];
  dataCompleteness: DataCompleteness;
  checkIn: DashboardCheckIn;
  pendingTriggerCount: number;
}

export function useDashboard() {
  return useQuery({
    queryKey: ['analytics', 'dashboard'],
    queryFn: () => apiRequest<Dashboard>('/analytics/dashboard'),
    // Analyses complete in the background; keep the view reasonably fresh.
    refetchInterval: (query) =>
      (query.state.data?.dataCompleteness.pendingAnalyses ?? 0) > 0 ? 5000 : false,
  });
}

// --- Presentation helpers (pure, so they can be unit tested) -------------------------

/** "+6" / "-4" / null when there is nothing to compare against. */
export function formatDelta(delta: number | null): string | null {
  if (delta === null || delta === undefined) {
    return null;
  }
  return delta > 0 ? `+${delta}` : `${delta}`;
}

export function deltaTone(delta: number | null): 'positive' | 'negative' | 'neutral' {
  if (delta === null || delta === undefined || delta === 0) {
    return 'neutral';
  }
  return delta > 0 ? 'positive' : 'negative';
}

/** Relative "last updated" text; null when nothing has ever been recorded. */
export function formatUpdatedAt(iso: string | null, now: Date = new Date()): string | null {
  if (!iso) {
    return null;
  }
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) {
    return null;
  }
  const minutes = Math.floor((now.getTime() - then) / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return days === 1 ? 'yesterday' : `${days}d ago`;
}

/** Bar height as a percentage; days without data render no bar at all. */
export function moodBarHeight(score: number | null): number | null {
  if (score === null || score === undefined) {
    return null;
  }
  return Math.max(4, Math.min(100, score));
}

/** True when the user has recorded nothing at all yet. */
export function isEmptyDashboard(completeness: DataCompleteness): boolean {
  return (
    !completeness.hasJournal &&
    !completeness.hasMood &&
    !completeness.hasAssessment &&
    !completeness.hasTriggers &&
    !completeness.hasAnalysis
  );
}

/** Days after which we suggest re-taking the questionnaire. */
export const REASSESS_AFTER_DAYS = 14;

/** Whether to nudge a re-assessment: never before one exists, then on a fortnightly cadence. */
export function shouldSuggestReassessment(completeness: DataCompleteness): boolean {
  if (!completeness.hasAssessment || completeness.daysSinceAssessment === null) {
    return false;
  }
  return completeness.daysSinceAssessment >= REASSESS_AFTER_DAYS;
}

/** Next best action for a user who hasn't filled the dashboard yet. */
export function nextStep(completeness: DataCompleteness): { label: string; to: string } | null {
  if (!completeness.hasJournal) {
    return { label: 'Write your first reflection', to: '/journal' };
  }
  if (!completeness.hasAssessment) {
    return { label: 'Take the wellbeing questionnaire', to: '/questionnaire' };
  }
  if (completeness.daysOfData < 7) {
    return { label: 'Add another reflection', to: '/journal' };
  }
  return null;
}
