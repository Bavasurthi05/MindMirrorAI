import { useQuery } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface MindsetComponent {
  label: string;
  value: number;
  /** Renormalized share of the score; components always sum to 1. */
  weight: number;
  contribution: number;
  detail: string;
}

export interface Mindset {
  /** Null when too few signals exist to compose a score — show an empty state, not a number. */
  score: number | null;
  band: string | null;
  components: MindsetComponent[];
  /** 0-1: how much evidence stands behind the score, not model certainty. */
  confidence: number;
  baselineEstablished: boolean;
  baselineDaysCovered: number;
  baselineMeanSentiment: number | null;
  baselineMeanMood: number | null;
  /** Standard deviations from this user's own normal. */
  deviationSigma: number | null;
  insight: string;
}

export function useMindset() {
  return useQuery({
    queryKey: ['analytics', 'mindset'],
    queryFn: () => apiRequest<Mindset>('/analytics/mindset'),
  });
}

export type BandTone = 'positive' | 'neutral' | 'caution' | 'concern' | 'unknown';

export function bandTone(band: string | null): BandTone {
  switch (band) {
    case 'Thriving':
      return 'positive';
    case 'Steady':
      return 'neutral';
    case 'Strained':
      return 'caution';
    case 'Struggling':
      return 'concern';
    default:
      return 'unknown';
  }
}

/** Plain-language confidence, so a 0-1 float never reaches the user. */
export function confidenceLabel(confidence: number): string {
  if (confidence >= 0.8) return 'High confidence';
  if (confidence >= 0.5) return 'Moderate confidence';
  if (confidence > 0) return 'Low confidence — still gathering data';
  return 'Not enough data';
}

/**
 * Deviation phrased in the user's terms.
 *
 * <p>Below 1σ we say nothing rather than dressing up noise as a finding.
 */
export function deviationLabel(sigma: number | null): string | null {
  if (sigma === null || sigma === undefined) {
    return null;
  }
  const magnitude = Math.abs(sigma);
  if (magnitude < 1) {
    return 'Within your usual range';
  }
  const direction = sigma < 0 ? 'below' : 'above';
  const strength = magnitude >= 2 ? 'well ' : '';
  return `${strength}${direction} your usual range`.replace(/^\w/, (c) => c.toUpperCase());
}
