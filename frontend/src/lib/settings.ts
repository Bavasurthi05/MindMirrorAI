export const REMINDER_KEY = 'mindmirror.journalReminder';

export type ReminderSettings = {
  isSupported: boolean;
  reminderOn: boolean;
  permission: NotificationPermission;
};

export function getStoredReminderPreference(storage: Storage | null = localStorage): boolean {
  if (!storage) {
    return false;
  }

  return storage.getItem(REMINDER_KEY) === 'true';
}

export function getReminderStatusMessage({ isSupported, reminderOn, permission }: ReminderSettings): string {
  if (!isSupported) {
    return 'Notifications are not supported in this browser.';
  }

  if (permission === 'denied') {
    return 'Notifications are blocked in your browser settings.';
  }

  return reminderOn ? 'Browser reminders are enabled.' : 'Get a gentle nudge to journal each day.';
}
