import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import { useTheme } from '../context/ThemeContext';
import { getReminderStatusMessage } from '../lib/settings';
import { detectTimezone, usePreferences, useUpdatePreferences } from '../lib/preferences';
import { notificationsSupported, useDailyReminder } from '../lib/reminder';

const panelClass =
  'rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-800';

export function SettingsPage() {
  const { theme, toggleTheme } = useTheme();
  const { data: preferences } = usePreferences();
  const update = useUpdatePreferences();
  const [permission, setPermission] = useState<NotificationPermission>('default');

  useEffect(() => {
    if (notificationsSupported()) {
      setPermission(Notification.permission);
    }
  }, []);

  const reminderOn = preferences?.reminderEnabled ?? false;
  const reminderTime = preferences?.reminderTime ?? '20:00';
  useDailyReminder(reminderOn, reminderTime);

  const toggleReminder = async () => {
    if (!notificationsSupported()) {
      return;
    }
    if (!reminderOn) {
      const result = await Notification.requestPermission();
      setPermission(result);
      if (result === 'granted') {
        update.mutate({ reminderEnabled: true });
      }
      return;
    }
    update.mutate({ reminderEnabled: false });
  };

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">Settings</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-900 dark:text-white">Preferences</h1>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-400">
          Adjust appearance, reminders and how your data is used.
        </p>

        <div className="mt-6 space-y-4">
          <div className={`flex items-center justify-between ${panelClass}`}>
            <div>
              <p className="text-sm font-semibold text-slate-900 dark:text-white">Appearance</p>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Current theme: {theme === 'dark' ? 'Dark' : 'Light'}
              </p>
            </div>
            <Button type="button" variant="secondary" onClick={toggleTheme}>
              Switch to {theme === 'dark' ? 'Light' : 'Dark'}
            </Button>
          </div>

          <div className={panelClass}>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-slate-900 dark:text-white">Daily check-in reminder</p>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  {getReminderStatusMessage({
                    isSupported: notificationsSupported(),
                    reminderOn,
                    permission,
                  })}
                </p>
                {permission === 'denied' ? (
                  <p className="mt-1 text-xs text-rose-600">Notifications are blocked in your browser settings.</p>
                ) : null}
              </div>
              <Button
                type="button"
                onClick={toggleReminder}
                disabled={!notificationsSupported() || permission === 'denied'}
              >
                {reminderOn ? 'Turn off' : 'Turn on'}
              </Button>
            </div>

            {reminderOn ? (
              <div className="mt-4 flex flex-wrap items-center gap-3">
                <label className="text-sm text-slate-700 dark:text-slate-200">
                  Remind me at
                  <input
                    type="time"
                    value={reminderTime}
                    onChange={(event) => update.mutate({ reminderTime: event.target.value })}
                    className="ml-2 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-sm dark:border-slate-600 dark:bg-slate-900"
                  />
                </label>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Reminders fire while MindMirror is open in a browser tab.
                </p>
              </div>
            ) : null}
          </div>

          <div className={panelClass}>
            <p className="text-sm font-semibold text-slate-900 dark:text-white">Timezone</p>
            <p className="text-sm text-slate-600 dark:text-slate-400">
              Used to decide when your day starts and ends for streaks and charts.
            </p>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <input
                value={preferences?.timezone ?? 'UTC'}
                onChange={(event) => update.mutate({ timezone: event.target.value })}
                className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-sm dark:border-slate-600 dark:bg-slate-900"
              />
              {preferences && preferences.timezone !== detectTimezone() ? (
                <button
                  type="button"
                  onClick={() => update.mutate({ timezone: detectTimezone() })}
                  className="text-xs font-semibold text-indigo-600 dark:text-indigo-300"
                >
                  Use detected ({detectTimezone()})
                </button>
              ) : null}
            </div>
            {update.isError ? (
              <p className="mt-2 text-xs text-rose-600">That timezone was not recognised.</p>
            ) : null}
          </div>

          <div className={panelClass}>
            <p className="text-sm font-semibold text-slate-900 dark:text-white">Help improve the model</p>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
              Optional and separate from your own analysis. Your text is stripped of names, emails, links
              and handles, and is never linked to your account when used.
            </p>
            <label className="mt-3 flex items-start gap-3 text-sm text-slate-700 dark:text-slate-200">
              <input
                type="checkbox"
                checked={preferences?.trainingConsent ?? false}
                onChange={(event) => update.mutate({ trainingConsent: event.target.checked })}
                className="mt-1 h-4 w-4"
              />
              <span>
                Use my entries to improve the model
                <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">
                  Withdrawing removes your entries from future training. Models already trained cannot be
                  un-trained. Declining does not change how the app works for you.
                </span>
              </span>
            </label>
          </div>
        </div>
      </motion.section>
    </div>
  );
}
