import { describe, expect, it } from 'vitest';
import { JOURNAL_PROMPTS, promptsForToday } from './prompts';
import { MOOD_FACES, faceForScore } from './checkin';
import { shouldSuggestReassessment, type DataCompleteness } from './dashboard';
import { retrainingReadiness, type FeedbackStats } from './admin';

function completeness(overrides: Partial<DataCompleteness> = {}): DataCompleteness {
  return {
    hasJournal: true,
    hasMood: true,
    hasAssessment: true,
    hasTriggers: true,
    hasAnalysis: true,
    daysOfData: 30,
    pendingAnalyses: 0,
    onboardingCompleted: true,
    daysSinceAssessment: 0,
    ...overrides,
  };
}

function stats(overrides: Partial<FeedbackStats> = {}): FeedbackStats {
  return {
    total: 0,
    agree: 0,
    disagree: 0,
    partial: 0,
    agreementRatePercent: null,
    correctedLabelCounts: {},
    ...overrides,
  };
}

describe('promptsForToday', () => {
  it('returns the requested number of distinct prompts', () => {
    const prompts = promptsForToday(3, new Date(2026, 7, 11));
    expect(prompts).toHaveLength(3);
    expect(new Set(prompts.map((p) => p.id)).size).toBe(3);
  });

  it('is stable within a day and changes across days', () => {
    const today = promptsForToday(3, new Date('2026-08-11T02:00:00Z'));
    const laterToday = promptsForToday(3, new Date('2026-08-11T20:00:00Z'));
    const tomorrow = promptsForToday(3, new Date('2026-08-12T10:00:00Z'));

    expect(today.map((p) => p.id)).toEqual(laterToday.map((p) => p.id));
    expect(today.map((p) => p.id)).not.toEqual(tomorrow.map((p) => p.id));
  });

  it('never asks for more prompts than exist', () => {
    expect(promptsForToday(99)).toHaveLength(JOURNAL_PROMPTS.length);
  });
});

describe('faceForScore', () => {
  it('has nothing to show without a score', () => {
    expect(faceForScore(null)).toBeNull();
  });

  it('maps a score onto the nearest face', () => {
    expect(faceForScore(95)?.label).toBe('Great');
    expect(faceForScore(52)?.label).toBe('Okay');
    expect(faceForScore(0)?.label).toBe('Rough');
  });

  it('round-trips every face', () => {
    for (const face of MOOD_FACES) {
      expect(faceForScore(face.score)?.label).toBe(face.label);
    }
  });
});

describe('shouldSuggestReassessment', () => {
  it('never nudges someone who has never been assessed', () => {
    expect(
      shouldSuggestReassessment(completeness({ hasAssessment: false, daysSinceAssessment: null })),
    ).toBe(false);
  });

  it('stays quiet inside the cadence window', () => {
    expect(shouldSuggestReassessment(completeness({ daysSinceAssessment: 13 }))).toBe(false);
  });

  it('nudges once the window has passed', () => {
    expect(shouldSuggestReassessment(completeness({ daysSinceAssessment: 14 }))).toBe(true);
  });
});

describe('retrainingReadiness', () => {
  it('is not ready with no feedback, and says why', () => {
    const result = retrainingReadiness(stats());
    expect(result.ready).toBe(false);
    expect(result.reasons.join(' ')).toContain('0/200');
    expect(result.reasons.join(' ')).toContain('no corrected labels');
  });

  it('flags classes that are too thin to train on', () => {
    const result = retrainingReadiness(
      stats({ total: 250, correctedLabelCounts: { anxiety: 40, normal: 5 } }),
    );
    expect(result.ready).toBe(false);
    expect(result.reasons.join(' ')).toContain('normal');
    expect(result.reasons.join(' ')).not.toContain('250/200');
  });

  it('is ready once volume and per-class balance are met', () => {
    const result = retrainingReadiness(
      stats({
        total: 300,
        correctedLabelCounts: { normal: 40, stress: 35, anxiety: 33, depression: 31 },
      }),
    );
    expect(result.ready).toBe(true);
    expect(result.reasons).toEqual([]);
  });
});
