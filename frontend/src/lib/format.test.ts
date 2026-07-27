import { describe, expect, it } from 'vitest';
import { relativeTime, stressTrend } from './format';

describe('relativeTime', () => {
  const now = new Date('2026-07-24T12:00:00Z').getTime();

  it('returns "just now" for very recent times', () => {
    expect(relativeTime('2026-07-24T11:59:40Z', now)).toBe('just now');
  });

  it('returns minutes for recent times', () => {
    expect(relativeTime('2026-07-24T11:30:00Z', now)).toBe('30 min ago');
  });

  it('returns hours within a day', () => {
    expect(relativeTime('2026-07-24T09:00:00Z', now)).toBe('3 hr ago');
  });

  it('returns days beyond a day', () => {
    expect(relativeTime('2026-07-22T12:00:00Z', now)).toBe('2 d ago');
  });
});

describe('stressTrend', () => {
  it('classifies intensity bands', () => {
    expect(stressTrend(8)).toBe('High');
    expect(stressTrend(5)).toBe('Medium');
    expect(stressTrend(2)).toBe('Low');
  });
});
