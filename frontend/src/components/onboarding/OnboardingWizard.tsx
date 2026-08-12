import { useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import {
  FOCUS_AREAS,
  detectTimezone,
  useUpdatePreferences,
  type UserPreferences,
} from '../../lib/preferences';

interface OnboardingWizardProps {
  preferences: UserPreferences;
}

/**
 * Four-step setup shown once, skippable and resumable.
 *
 * <p>Collects the things the app cannot infer: the user's timezone (every daily bucket depends
 * on it), when to nudge them, what they care about, and an explicit answer on training consent.
 */
export function OnboardingWizard({ preferences }: OnboardingWizardProps) {
  const update = useUpdatePreferences();
  const [step, setStep] = useState(preferences.onboardingStep || 0);
  const [timezone, setTimezone] = useState(
    preferences.timezone === 'UTC' ? detectTimezone() : preferences.timezone,
  );
  const [reminderEnabled, setReminderEnabled] = useState(preferences.reminderEnabled);
  const [reminderTime, setReminderTime] = useState(preferences.reminderTime);
  const [focusAreas, setFocusAreas] = useState<string[]>(preferences.focusAreas);
  const [trainingConsent, setTrainingConsent] = useState(preferences.trainingConsent);

  const goTo = (next: number) => {
    setStep(next);
    update.mutate({ onboardingStep: next });
  };

  const finish = () => {
    update.mutate({
      timezone,
      reminderEnabled,
      reminderTime,
      focusAreas,
      trainingConsent,
      onboardingCompleted: true,
      onboardingStep: 4,
    });
  };

  const skip = () => update.mutate({ timezone, onboardingCompleted: true });

  const toggleFocus = (area: string) =>
    setFocusAreas((current) =>
      current.includes(area) ? current.filter((item) => item !== area) : [...current, area].slice(0, 8),
    );

  return (
    <motion.section
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-[1.5rem] border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
    >
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-300">
            Step {step + 1} of 4
          </p>
          <h2 className="mt-1 text-xl font-semibold text-slate-900 dark:text-white">
            {['Where are you?', 'Daily reminder', 'What matters most?', 'Helping the model'][step]}
          </h2>
        </div>
        <button
          type="button"
          onClick={skip}
          className="rounded-lg px-2 py-1 text-xs text-slate-500 transition hover:bg-slate-100 dark:hover:bg-slate-800"
        >
          Skip setup
        </button>
      </div>

      <div className="mt-5">
        {step === 0 ? (
          <div>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Your timezone decides when a day starts and ends for streaks and charts.
            </p>
            <label className="mt-3 block text-sm font-medium text-slate-700 dark:text-slate-200">
              Timezone
              <input
                value={timezone}
                onChange={(event) => setTimezone(event.target.value)}
                className="mt-1 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-800"
              />
            </label>
            <p className="mt-2 text-xs text-slate-500">Detected: {detectTimezone()}</p>
          </div>
        ) : null}

        {step === 1 ? (
          <div>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              A gentle nudge to check in. You can turn this off any time in Settings.
            </p>
            <label className="mt-3 flex items-center gap-3 text-sm text-slate-700 dark:text-slate-200">
              <input
                type="checkbox"
                checked={reminderEnabled}
                onChange={(event) => setReminderEnabled(event.target.checked)}
                className="h-4 w-4"
              />
              Remind me daily
            </label>
            {reminderEnabled ? (
              <label className="mt-3 block text-sm font-medium text-slate-700 dark:text-slate-200">
                Time
                <input
                  type="time"
                  value={reminderTime}
                  onChange={(event) => setReminderTime(event.target.value)}
                  className="mt-1 block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-800"
                />
              </label>
            ) : null}
          </div>
        ) : null}

        {step === 2 ? (
          <div>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Pick a few areas you would like to keep an eye on.
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              {FOCUS_AREAS.map((area) => (
                <button
                  key={area}
                  type="button"
                  onClick={() => toggleFocus(area)}
                  aria-pressed={focusAreas.includes(area)}
                  className={`rounded-full border px-3 py-1.5 text-sm transition ${
                    focusAreas.includes(area)
                      ? 'border-indigo-500 bg-indigo-50 font-semibold text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-200'
                      : 'border-slate-300 text-slate-700 hover:border-indigo-300 dark:border-slate-600 dark:text-slate-200'
                  }`}
                >
                  {area}
                </button>
              ))}
            </div>
          </div>
        ) : null}

        {step === 3 ? (
          <div>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              You can let your entries help improve the model that generates everyone&rsquo;s insights.
              This is entirely optional and separate from your own analysis.
            </p>
            <label className="mt-4 flex items-start gap-3 rounded-2xl border border-slate-200 p-4 text-sm dark:border-slate-700">
              <input
                type="checkbox"
                checked={trainingConsent}
                onChange={(event) => setTrainingConsent(event.target.checked)}
                className="mt-1 h-4 w-4"
              />
              <span className="text-slate-700 dark:text-slate-200">
                <span className="font-semibold">Use my entries to improve the model</span>
                <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">
                  Your text is stripped of names, emails, links and handles, and is never linked to your
                  account when used. You can withdraw this at any time, which removes your entries from
                  future training — models already trained cannot be un-trained.
                </span>
              </span>
            </label>
            <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">
              Saying no changes nothing about how the app works for you.
            </p>
          </div>
        ) : null}
      </div>

      <div className="mt-6 flex flex-wrap items-center gap-2">
        {step > 0 ? (
          <button
            type="button"
            onClick={() => goTo(step - 1)}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200"
          >
            Back
          </button>
        ) : null}
        {step < 3 ? (
          <button
            type="button"
            onClick={() => goTo(step + 1)}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500"
          >
            Continue
          </button>
        ) : (
          <button
            type="button"
            onClick={finish}
            disabled={update.isPending}
            className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:opacity-60"
          >
            {update.isPending ? 'Saving…' : 'Finish setup'}
          </button>
        )}
        <Link to="/journal" className="ml-auto text-sm font-semibold text-indigo-600 dark:text-indigo-300">
          Or just start writing →
        </Link>
      </div>
    </motion.section>
  );
}
