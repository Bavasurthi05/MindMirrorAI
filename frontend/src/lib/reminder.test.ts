import { describe, expect, it } from 'vitest';
import { alreadyFiredToday, markFired, msUntilNext } from './reminder';

function fakeStorage(): Storage {
  const data = new Map<string, string>();
  return {
    getItem: (key: string) => data.get(key) ?? null,
    setItem: (key: string, value: string) => void data.set(key, value),
    removeItem: (key: string) => void data.delete(key),
    clear: () => data.clear(),
    key: () => null,
    length: 0,
  } as unknown as Storage;
}

describe('msUntilNext', () => {
  it('counts forward to a time later today', () => {
    const now = new Date(2026, 7, 11, 9, 0, 0);
    expect(msUntilNext('20:00', now)).toBe(11 * 60 * 60 * 1000);
  });

  it('rolls over to tomorrow once the time has passed', () => {
    const now = new Date(2026, 7, 11, 21, 0, 0);
    expect(msUntilNext('20:00', now)).toBe(23 * 60 * 60 * 1000);
  });

  it('treats the exact minute as already passed so it does not fire twice', () => {
    const now = new Date(2026, 7, 11, 20, 0, 0);
    expect(msUntilNext('20:00', now)).toBe(24 * 60 * 60 * 1000);
  });

  it('returns null for a malformed time rather than scheduling at NaN', () => {
    expect(msUntilNext('25:00')).toBeNull();
    expect(msUntilNext('8:00')).toBeNull();
    expect(msUntilNext('')).toBeNull();
  });
});

describe('alreadyFiredToday', () => {
  it('is false before anything fires', () => {
    expect(alreadyFiredToday(new Date(2026, 7, 11), fakeStorage())).toBe(false);
  });

  it('is true for the same day after firing', () => {
    const storage = fakeStorage();
    const day = new Date(2026, 7, 11, 20, 0, 0);
    markFired(day, storage);
    expect(alreadyFiredToday(new Date(2026, 7, 11, 22, 0, 0), storage)).toBe(true);
  });

  it('resets on the next day', () => {
    const storage = fakeStorage();
    markFired(new Date(2026, 7, 11), storage);
    expect(alreadyFiredToday(new Date(2026, 7, 12), storage)).toBe(false);
  });
});
