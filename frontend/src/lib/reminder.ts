import { useEffect } from 'react';

/**
 * Daily check-in reminder.
 *
 * <p>Scope note: this schedules a notification **while the app is open in a tab**. Firing when
 * the browser is closed needs a service worker plus a push subscription and server-side push
 * keys, which this app does not have — so the UI says so rather than implying otherwise.
 * Previously the toggle stored a flag and showed one notification immediately; nothing ever
 * recurred.
 */

const LAST_FIRED_KEY = 'mindmirror.reminderLastFired';

/** Milliseconds from `now` until the next occurrence of local "HH:mm". */
export function msUntilNext(time: string, now: Date = new Date()): number | null {
  const match = /^([01]\d|2[0-3]):([0-5]\d)$/.exec(time);
  if (!match) {
    return null;
  }
  const [hours, minutes] = [Number(match[1]), Number(match[2])];
  const target = new Date(now);
  target.setHours(hours, minutes, 0, 0);
  if (target.getTime() <= now.getTime()) {
    target.setDate(target.getDate() + 1);
  }
  return target.getTime() - now.getTime();
}

/** Guards against re-notifying on the same local day after a reload. */
export function alreadyFiredToday(now: Date = new Date(), storage: Storage | null = localStorage): boolean {
  if (!storage) {
    return false;
  }
  return storage.getItem(LAST_FIRED_KEY) === now.toDateString();
}

export function markFired(now: Date = new Date(), storage: Storage | null = localStorage): void {
  storage?.setItem(LAST_FIRED_KEY, now.toDateString());
}

export function notificationsSupported(): boolean {
  return typeof window !== 'undefined' && 'Notification' in window;
}

/**
 * Schedules the reminder for as long as the component stays mounted.
 *
 * <p>setTimeout rather than an interval so it survives clock drift, and it re-arms itself for
 * the following day after firing.
 */
export function useDailyReminder(enabled: boolean, time: string): void {
  useEffect(() => {
    if (!enabled || !notificationsSupported() || Notification.permission !== 'granted') {
      return;
    }

    let timer: number | undefined;

    const arm = () => {
      const delay = msUntilNext(time);
      if (delay === null) {
        return;
      }
      timer = window.setTimeout(() => {
        if (!alreadyFiredToday()) {
          new Notification('MindMirror AI', {
            body: 'A minute to check in? It keeps your insights current. 📝',
          });
          markFired();
        }
        arm(); // re-arm for tomorrow
      }, delay);
    };

    arm();
    return () => {
      if (timer !== undefined) {
        window.clearTimeout(timer);
      }
    };
  }, [enabled, time]);
}
