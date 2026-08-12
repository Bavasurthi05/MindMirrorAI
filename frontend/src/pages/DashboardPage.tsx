import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { useProfile } from '../lib/profile';
import { useGoals } from '../lib/goals';
import { useAuth } from '../context/AuthContext';
import {
  REASSESS_AFTER_DAYS,
  deltaTone,
  formatDelta,
  formatUpdatedAt,
  isEmptyDashboard,
  moodBarHeight,
  nextStep,
  shouldSuggestReassessment,
  useDashboard,
} from '../lib/dashboard';
import { Skeleton } from '../components/feedback/Skeleton';
import { CheckInCard } from '../components/checkin/CheckInCard';
import { TriggerConfirmation } from '../components/triggers/TriggerConfirmation';
import { OnboardingWizard } from '../components/onboarding/OnboardingWizard';
import { usePreferences } from '../lib/preferences';
import { MindsetPanel } from '../components/mindset/MindsetPanel';

// Navigation, not data — these are links, so they are legitimately static.
const quickActions = [
  { title: 'New journal entry', to: '/journal', icon: '📝' },
  { title: 'Run questionnaire', to: '/questionnaire', icon: '🧪' },
  { title: 'View mirror', to: '/mirror', icon: '🪞' },
  { title: 'Open reports', to: '/reports', icon: '📊' },
];

const cardClass =
  'rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900';
const eyebrowClass = 'text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-300';

/** Shown wherever a metric has no data behind it yet. */
function NoData({ hint }: { hint: string }) {
  return <p className="text-sm text-slate-500 dark:text-slate-400">{hint}</p>;
}

export function DashboardPage() {
  const { user } = useAuth();
  const { data: profile } = useProfile();
  const { data: goals } = useGoals();
  const { data: dashboard, isLoading } = useDashboard();
  const { data: preferences } = usePreferences();

  const displayName = (profile?.fullName ?? user?.fullName ?? '').trim();
  const firstName = displayName.split(/\s+/)[0] || 'there';

  const completeness = dashboard?.dataCompleteness;
  const empty = completeness ? isEmptyDashboard(completeness) : false;
  const step = completeness ? nextStep(completeness) : null;
  const delta = formatDelta(dashboard?.wellnessDelta ?? null);
  const tone = deltaTone(dashboard?.wellnessDelta ?? null);
  const updatedAt = formatUpdatedAt(dashboard?.wellnessUpdatedAt ?? null);

  const showOnboarding = Boolean(preferences && !preferences.onboardingCompleted);
  const suggestReassessment = completeness ? shouldSuggestReassessment(completeness) : false;

  return (
    <div className="space-y-6">
      {showOnboarding && preferences ? <OnboardingWizard preferences={preferences} /> : null}

      {dashboard && !showOnboarding ? <CheckInCard checkIn={dashboard.checkIn} /> : null}

      {dashboard && dashboard.pendingTriggerCount > 0 ? <TriggerConfirmation compact /> : null}

      {suggestReassessment ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm dark:border-slate-800 dark:bg-slate-900">
          <span className="text-slate-700 dark:text-slate-200">
            It has been {completeness?.daysSinceAssessment} days since your last questionnaire — retaking it
            every {REASSESS_AFTER_DAYS} days keeps your wellness score current.
          </span>
          <Link
            to="/questionnaire"
            className="rounded-xl bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white transition hover:bg-indigo-500"
          >
            Retake it
          </Link>
        </div>
      ) : null}

      {completeness && completeness.pendingAnalyses > 0 ? (
        <div className="rounded-2xl border border-cyan-200 bg-cyan-50 px-5 py-3 text-sm text-cyan-800 dark:border-cyan-900/50 dark:bg-cyan-950/40 dark:text-cyan-200">
          Analyzing {completeness.pendingAnalyses} recent{' '}
          {completeness.pendingAnalyses === 1 ? 'entry' : 'entries'} — insights will appear shortly.
        </div>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-[1.5rem] border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <p className="text-sm text-slate-500 dark:text-slate-400">🔥 Journal streak</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">
            {profile ? `${profile.journalStreak} day${profile.journalStreak === 1 ? '' : 's'}` : '—'}
          </p>
        </div>
        <div className="rounded-[1.5rem] border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <p className="text-sm text-slate-500 dark:text-slate-400">📝 Journal entries</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">{profile?.journalCount ?? '—'}</p>
        </div>
        <div className="rounded-[1.5rem] border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <p className="text-sm text-slate-500 dark:text-slate-400">🎯 Goals completed</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">
            {profile ? `${profile.goalsCompleted}/${profile.goalsTotal}` : '—'}
          </p>
        </div>
        <Link
          to="/profile"
          className="flex flex-col justify-center rounded-[1.5rem] border border-indigo-200 bg-indigo-50 p-5 shadow-sm transition hover:border-indigo-300 dark:border-indigo-900/50 dark:bg-indigo-950/40"
        >
          <p className="text-sm font-semibold text-indigo-700 dark:text-indigo-200">View goals &amp; streaks →</p>
          <p className="mt-1 text-xs text-indigo-500 dark:text-indigo-300">
            {goals ? `${goals.length} active goal${goals.length === 1 ? '' : 's'}` : 'Manage your goals'}
          </p>
        </Link>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
          className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
        >
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Welcome back</p>
              <h1 className="mt-2 text-3xl font-semibold">{firstName}</h1>
              <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-300">
                {isLoading ? 'Loading your latest insights…' : dashboard?.headline}
              </p>
              {dashboard?.mindsetInsight ? (
                <p className="mt-2 max-w-2xl text-sm leading-7 text-cyan-200">{dashboard.mindsetInsight}</p>
              ) : null}
              {step ? (
                <Link
                  to={step.to}
                  className="mt-4 inline-block rounded-xl bg-white/10 px-4 py-2 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20"
                >
                  {step.label} →
                </Link>
              ) : null}
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 text-center backdrop-blur">
              {isLoading ? (
                <Skeleton className="h-9 w-16 bg-white/20" />
              ) : (
                <p className="text-3xl font-semibold">{dashboard?.wellnessScore ?? '—'}</p>
              )}
              <p className="text-sm text-slate-300">{dashboard?.mindsetBand ?? 'Wellness score'}</p>
              {delta ? (
                <p
                  className={`mt-1 text-xs font-semibold ${
                    tone === 'positive'
                      ? 'text-emerald-300'
                      : tone === 'negative'
                        ? 'text-rose-300'
                        : 'text-slate-300'
                  }`}
                >
                  {delta} vs. last week
                </p>
              ) : null}
              {updatedAt ? <p className="mt-1 text-xs text-slate-400">Updated {updatedAt}</p> : null}
            </div>
          </div>
        </motion.section>

        <MindsetPanel />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <section className={cardClass}>
          <p className={eyebrowClass}>Mental Health Summary</p>
          <div className="mt-6 grid gap-4 md:grid-cols-3">
            {[
              { label: 'Stress', value: dashboard?.summary.stress, hint: 'No triggers recorded' },
              { label: 'Energy', value: dashboard?.summary.energy, hint: 'No mood data' },
              { label: 'Reflection', value: dashboard?.summary.reflection, hint: 'No entries yet' },
            ].map((item) => (
              <div key={item.label} className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-800/60">
                <p className="text-sm text-slate-500 dark:text-slate-400">{item.label}</p>
                <p className="mt-1 text-xl font-semibold text-slate-900 dark:text-white">
                  {item.value ?? <span className="text-base font-normal text-slate-400">{item.hint}</span>}
                </p>
              </div>
            ))}
          </div>
        </section>

        <section className={cardClass}>
          <p className={eyebrowClass}>Recent Analysis</p>
          <div className="mt-6 space-y-3">
            {isLoading ? (
              <Skeleton className="h-20 w-full" />
            ) : dashboard?.recentAnalyses.length ? (
              dashboard.recentAnalyses.map((item) => (
                <div
                  key={item.id}
                  className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/60"
                >
                  <p className="text-sm font-semibold text-slate-900 dark:text-white">{item.headline}</p>
                  <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{item.detail}</p>
                </div>
              ))
            ) : (
              <NoData hint="Your reflections are analyzed automatically — write an entry to see insights here." />
            )}
          </div>
        </section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <section className={cardClass}>
          <p className={eyebrowClass}>Quick Actions</p>
          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            {quickActions.map((action) => (
              <Link
                key={action.title}
                to={action.to}
                className="rounded-2xl border border-slate-200 bg-slate-50 p-4 transition hover:border-indigo-300 hover:bg-indigo-50 dark:border-slate-700 dark:bg-slate-800/60"
              >
                <div className="text-2xl">{action.icon}</div>
                <p className="mt-3 font-semibold text-slate-900 dark:text-white">{action.title}</p>
              </Link>
            ))}
          </div>
        </section>

        <section className={cardClass}>
          <p className={eyebrowClass}>Mood Overview</p>
          <h2 className="mt-2 text-sm text-slate-500 dark:text-slate-400">The last 7 days</h2>
          <div className="mt-6 grid grid-cols-7 gap-2">
            {(dashboard?.moodWeek ?? []).map((day) => {
              const height = moodBarHeight(day.score);
              return (
                <div key={day.date} className="rounded-2xl bg-slate-50 p-2 text-center dark:bg-slate-800/60">
                  <p className="text-xs text-slate-500 dark:text-slate-400">{day.label}</p>
                  <div className="mt-2 flex h-20 items-end justify-center">
                    {height === null ? (
                      <div className="h-full w-full rounded-xl border border-dashed border-slate-300 dark:border-slate-700" />
                    ) : (
                      <div
                        className="w-full rounded-xl bg-gradient-to-t from-indigo-500 to-cyan-400"
                        style={{ height: `${height}%` }}
                        title={`${day.score}/100`}
                      />
                    )}
                  </div>
                  <p className="mt-1 text-xs font-semibold text-slate-700 dark:text-slate-300">
                    {day.score ?? '–'}
                  </p>
                </div>
              );
            })}
          </div>
          {empty ? (
            <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
              Mood is inferred from your reflections, or you can log it directly.
            </p>
          ) : null}
        </section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <section className={cardClass}>
          <p className={eyebrowClass}>Recommendations</p>
          <div className="mt-6 space-y-3">
            {dashboard?.recommendations.length ? (
              dashboard.recommendations.map((item) => (
                <div
                  key={item.id}
                  className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/60"
                >
                  <p className="font-semibold text-slate-900 dark:text-white">{item.title}</p>
                  {item.detail ? (
                    <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{item.detail}</p>
                  ) : null}
                </div>
              ))
            ) : (
              <NoData hint="Your recovery plan builds after your first assessment." />
            )}
          </div>
          <Link
            to="/recommendations"
            className="mt-4 inline-block text-sm font-semibold text-indigo-600 dark:text-indigo-300"
          >
            Open recovery plan →
          </Link>
        </section>

        <section className={cardClass}>
          <p className={eyebrowClass}>Trigger Summary</p>
          <div className="mt-6 space-y-3">
            {dashboard?.triggerSummary.length ? (
              dashboard.triggerSummary.map((item) => (
                <div
                  key={item.label}
                  className="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3 dark:bg-slate-800/60"
                >
                  <span className="text-sm text-slate-700 dark:text-slate-300">{item.label}</span>
                  <span className="text-sm font-semibold text-slate-900 dark:text-white">{item.value}</span>
                </div>
              ))
            ) : (
              <NoData hint="Triggers are detected in your writing, or you can log them yourself." />
            )}
          </div>
        </section>
      </div>

      <section className={cardClass}>
        <p className={eyebrowClass}>Wellness Progress</p>
        <div className="mt-6 space-y-4">
          {(dashboard?.progressItems ?? []).map((item) => (
            <div key={item.label}>
              <div className="mb-2 flex items-center justify-between text-sm text-slate-600 dark:text-slate-300">
                <span>{item.label}</span>
                <span className="font-semibold text-slate-900 dark:text-white">
                  {item.progress === null ? 'Not enough data' : `${item.progress}%`}
                </span>
              </div>
              <div className="h-2 rounded-full bg-slate-200 dark:bg-slate-700">
                {item.progress === null ? null : (
                  <div
                    className="h-2 rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500"
                    style={{ width: `${item.progress}%` }}
                  />
                )}
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
