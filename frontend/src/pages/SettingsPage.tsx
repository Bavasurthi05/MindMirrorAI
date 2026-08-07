import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import { useTheme } from '../context/ThemeContext';
import { getReminderStatusMessage, getStoredReminderPreference, REMINDER_KEY } from '../lib/settings';

export function SettingsPage() {
  const { theme, toggleTheme } = useTheme();
  const [reminderOn, setReminderOn] = useState(false);
  const [permission, setPermission] = useState<NotificationPermission>('default');

  useEffect(() => {
    setReminderOn(getStoredReminderPreference(localStorage));
    if ('Notification' in window) {
      setPermission(Notification.permission);
    }
  }, []);

  const toggleReminder = async () => {
    if (!('Notification' in window)) {
      return;
    }
    if (!reminderOn) {
      const result = await Notification.requestPermission();
      setPermission(result);
      if (result === 'granted') {
        localStorage.setItem(REMINDER_KEY, 'true');
        setReminderOn(true);
        new Notification('MindMirror AI', { body: 'Daily journal reminders are on. See you tomorrow! 📝' });
      }
    } else {
      localStorage.removeItem(REMINDER_KEY);
      setReminderOn(false);
    }
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
          Adjust appearance and reminders to fit your routine.
        </p>

        <div className="mt-6 space-y-4">
          <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-800">
            <div>
              <p className="text-sm font-semibold text-slate-900 dark:text-white">Appearance</p>
              <p className="text-sm text-slate-600 dark:text-slate-400">Current theme: {theme === 'dark' ? 'Dark' : 'Light'}</p>
            </div>
            <Button type="button" variant="secondary" onClick={toggleTheme}>
              Switch to {theme === 'dark' ? 'Light' : 'Dark'}
            </Button>
          </div>

          <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-800">
            <div>
              <p className="text-sm font-semibold text-slate-900 dark:text-white">Daily journal reminder</p>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                {getReminderStatusMessage({
                  isSupported: 'Notification' in window,
                  reminderOn,
                  permission,
                })}
              </p>
              {permission === 'denied' ? (
                <p className="mt-1 text-xs text-rose-600">Notifications are blocked in your browser settings.</p>
              ) : null}
            </div>
            <Button type="button" onClick={toggleReminder} disabled={!('Notification' in window) || permission === 'denied'}>
              {reminderOn ? 'Turn off' : 'Turn on'}
            </Button>
          </div>
        </div>
      </motion.section>
    </div>
  );
}
