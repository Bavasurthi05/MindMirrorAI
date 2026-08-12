import { describe, expect, it } from 'vitest';
import {
  deltaTone,
  formatDelta,
  formatUpdatedAt,
  isEmptyDashboard,
  moodBarHeight,
  nextStep,
  type DataCompleteness,
} from './dashboard';

function completeness(overrides: Partial<DataCompleteness> = {}): DataCompleteness {
  return {
    hasJournal: false,
    hasMood: false,
    hasAssessment: false,
    hasTriggers: false,
    hasAnalysis: false,
    daysOfData: 0,
    pendingAnalyses: 0,
    ...overrides,
  };
}

describe('formatDelta', () => {
  it('signs positive changes', () => {
    expect(formatDelta(6)).toBe('+6');
  });

  it('keeps the minus on negative changes', () => {
    expect(formatDelta(-4)).toBe('-4');
  });

  it('returns null when there is nothing to compare', () => {
    expect(formatDelta(null)).toBeNull();
  });
});

describe('deltaTone', () => {
  it('treats zero and null as neutral', () => {
    expect(deltaTone(0)).toBe('neutral');
    expect(deltaTone(null)).toBe('neutral');
  });

  it('distinguishes improvement from decline', () => {
    expect(deltaTone(3)).toBe('positive');
    expect(deltaTone(-3)).toBe('negative');
  });
});

describe('formatUpdatedAt', () => {
  const now = new Date('2026-08-11T12:00:00Z');

  it('returns null when nothing has been recorded', () => {
    expect(formatUpdatedAt(null, now)).toBeNull();
  });

  it('returns null for an unparseable timestamp rather than NaN text', () => {
    expect(formatUpdatedAt('not-a-date', now)).toBeNull();
  });

  it('describes recent activity in minutes and hours', () => {
    expect(formatUpdatedAt('2026-08-11T11:58:00Z', now)).toBe('2m ago');
    expect(formatUpdatedAt('2026-08-11T09:00:00Z', now)).toBe('3h ago');
  });

  it('describes older activity in days', () => {
    expect(formatUpdatedAt('2026-08-10T12:00:00Z', now)).toBe('yesterday');
    expect(formatUpdatedAt('2026-08-08T12:00:00Z', now)).toBe('3d ago');
  });
});

describe('moodBarHeight', () => {
  it('renders no bar for a day with no entry', () => {
    expect(moodBarHeight(null)).toBeNull();
  });

  it('keeps a low score visible instead of collapsing to nothing', () => {
    expect(moodBarHeight(0)).toBe(4);
  });

  it('clamps to the track', () => {
    expect(moodBarHeight(150)).toBe(100);
    expect(moodBarHeight(62)).toBe(62);
  });
});

describe('isEmptyDashboard', () => {
  it('is true for a brand new account', () => {
    expect(isEmptyDashboard(completeness())).toBe(true);
  });

  it('is false once any signal exists', () => {
    expect(isEmptyDashboard(completeness({ hasMood: true }))).toBe(false);
  });
});

describe('nextStep', () => {
  it('sends a new user to write their first reflection', () => {
    expect(nextStep(completeness())).toEqual({
      label: 'Write your first reflection',
      to: '/journal',
    });
  });

  it('asks for a questionnaire once journaling has started', () => {
    expect(nextStep(completeness({ hasJournal: true }))?.to).toBe('/questionnaire');
  });

  it('nudges for more data during the first week', () => {
    expect(nextStep(completeness({ hasJournal: true, hasAssessment: true, daysOfData: 3 }))?.to).toBe(
      '/journal',
    );
  });

  it('stops nudging once there is a week of data', () => {
    expect(
      nextStep(completeness({ hasJournal: true, hasAssessment: true, daysOfData: 9 })),
    ).toBeNull();
  });
});
