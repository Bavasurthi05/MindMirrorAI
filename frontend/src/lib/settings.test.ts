import { describe, expect, it } from 'vitest';
import { getReminderStatusMessage, getStoredReminderPreference, REMINDER_KEY } from './settings';

function createStorage(value: string | null) {
  const store = new Map<string, string>();
  if (value !== null) {
    store.set(REMINDER_KEY, value);
  }

  return {
    getItem(key: string) {
      return store.has(key) ? store.get(key) ?? null : null;
    },
    setItem(key: string, value: string) {
      store.set(key, value);
    },
    removeItem(key: string) {
      store.delete(key);
    },
  } as Storage;
}

describe('getStoredReminderPreference', () => {
  it('reads true values from storage and defaults to false otherwise', () => {
    expect(getStoredReminderPreference(createStorage('true'))).toBe(true);
    expect(getStoredReminderPreference(createStorage('false'))).toBe(false);
    expect(getStoredReminderPreference(createStorage(null))).toBe(false);
  });
});

describe('getReminderStatusMessage', () => {
  it('returns a clear message when notifications are blocked', () => {
    expect(
      getReminderStatusMessage({ isSupported: true, reminderOn: false, permission: 'denied' }),
    ).toContain('blocked');
  });
});
