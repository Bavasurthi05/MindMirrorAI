import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { useAnalyticsOverview } from '../lib/analytics';
import { EmptyState } from '../components/feedback/EmptyState';
import { MindsetPanel } from '../components/mindset/MindsetPanel';

const RADAR_LABELS = ['Stress', 'Confidence', 'Sleep', 'Motivation', 'Social', 'Happiness'];

function CircularProgress({ value, label }: { value: number; label: string }) {
  const radius = 42;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;

  return (
    <div className="flex flex-col items-center">
      <div className="relative flex h-28 w-28 items-center justify-center">
        <svg viewBox="0 0 120 120" className="h-28 w-28 -rotate-90">
          <circle cx="60" cy="60" r={radius} stroke="#e2e8f0" strokeWidth="10" fill="none" />
          <circle
            cx="60"
            cy="60"
            r={radius}
            stroke="url(#gradient)"
            strokeWidth="10"
            strokeLinecap="round"
            fill="none"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
          />
          <defs>
            <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#22d3ee" />
              <stop offset="100%" stopColor="#6366f1" />
            </linearGradient>
          </defs>
        </svg>
        <div className="absolute text-center">
          <p className="text-xl font-semibold text-slate-900">{value}%</p>
        </div>
      </div>
      <p className="mt-3 text-sm font-medium text-slate-600">{label}</p>
    </div>
  );
}

export function MirrorPage() {
  const { data, isLoading } = useAnalyticsOverview();

  const radarByLabel = new Map((data?.radar ?? []).map((m) => [m.label, m.value]));
  // No invented fallbacks: an account with no data must not see a plausible-looking profile.
  const hasRadar = radarByLabel.size > 0;
  const radarValues = RADAR_LABELS.map((label) => radarByLabel.get(label) ?? 0);
  const overallWellness = data?.overallWellness ?? null;

  const metrics = hasRadar
    ? [
        ...(overallWellness === null
          ? []
          : [{ label: 'Wellness Score', value: `${overallWellness}%`, tone: 'from-cyan-500 to-indigo-500' }]),
        ...RADAR_LABELS.map((label, index) => ({
          label,
          value: `${radarValues[index]}%`,
          tone: 'from-indigo-500 to-violet-500',
        })),
      ]
    : [];

  const circularStats = hasRadar
    ? (['Happiness', 'Confidence', 'Motivation'] as const)
        .filter((label) => radarByLabel.has(label))
        .map((label) => ({ label, value: Math.round(radarByLabel.get(label) as number) }))
    : [];

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
      >
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Mental Health Digital Mirror</p>
            <h1 className="mt-2 text-3xl font-semibold">A reflective view of your inner wellbeing</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-300">
              A modern, visually rich summary of your wellbeing indicators, derived from your recent check-ins,
              triggers, and assessments.
            </p>
          </div>
          <div className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 text-center backdrop-blur">
            <p className="text-4xl font-semibold">{overallWellness ?? '—'}</p>
            <p className="text-sm text-slate-300">Overall wellness</p>
          </div>
        </div>
      </motion.section>

      <MindsetPanel />

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.05 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Radar Chart</p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-900">Emotional profile snapshot</h2>
            </div>
          </div>

          {!hasRadar ? (
            <div className="mt-6">
              <EmptyState
                title={isLoading ? 'Loading your profile…' : 'Not enough data yet'}
                description={
                  isLoading
                    ? undefined
                    : 'Your emotional profile is built from your reflections, mood check-ins, triggers and assessments. Add a few entries and it will appear here.'
                }
                action={
                  isLoading ? undefined : (
                    <Link
                      to="/journal"
                      className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500"
                    >
                      Write a reflection
                    </Link>
                  )
                }
              />
            </div>
          ) : (
          <div className="mt-6 rounded-[1.5rem] border border-slate-200 bg-slate-50 p-4">
            <div className="mx-auto flex max-w-md items-center justify-center">
              <svg viewBox="0 0 240 240" className="h-[280px] w-[280px]">
                {[20, 40, 60, 80, 100].map((level) => (
                  <polygon
                    key={level}
                    points={Array.from({ length: 6 }, (_, index) => {
                      const angle = (Math.PI / 3) * index - Math.PI / 2;
                      const radius = (level / 100) * 90;
                      return `${120 + Math.cos(angle) * radius},${120 + Math.sin(angle) * radius}`;
                    }).join(' ')}
                    fill="none"
                    stroke="#e2e8f0"
                    strokeWidth="1"
                  />
                ))}
                <polygon
                  points={Array.from({ length: 6 }, (_, index) => {
                    const angle = (Math.PI / 3) * index - Math.PI / 2;
                    const radius = (radarValues[index] / 100) * 90;
                    return `${120 + Math.cos(angle) * radius},${120 + Math.sin(angle) * radius}`;
                  }).join(' ')}
                  fill="rgba(99, 102, 241, 0.25)"
                  stroke="#6366f1"
                  strokeWidth="3"
                />
                {RADAR_LABELS.map((label, index) => {
                  const angle = (Math.PI / 3) * index - Math.PI / 2;
                  const x = 120 + Math.cos(angle) * 105;
                  const y = 120 + Math.sin(angle) * 105;
                  return (
                    <text key={label} x={x} y={y} textAnchor="middle" dominantBaseline="middle" fontSize="11" fill="#64748b">
                      {label}
                    </text>
                  );
                })}
              </svg>
            </div>
          </div>
          )}
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Wellness Snapshot</p>
          {metrics.length === 0 ? (
            <p className="mt-6 text-sm text-slate-500">
              Metrics appear once you have recorded some reflections or check-ins.
            </p>
          ) : null}
          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            {metrics.map((item) => (
              <motion.div
                key={item.label}
                whileHover={{ y: -3, scale: 1.01 }}
                className="rounded-2xl border border-slate-200 bg-slate-50 p-4"
              >
                <div className={`h-1.5 rounded-full bg-gradient-to-r ${item.tone}`} />
                <p className="mt-3 text-sm font-semibold text-slate-900">{item.label}</p>
                <p className="mt-1 text-sm text-slate-600">{item.value}</p>
              </motion.div>
            ))}
          </div>
        </motion.section>
      </div>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.12 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Circular Progress Indicators</p>
        {circularStats.length === 0 ? (
          <p className="mt-6 text-sm text-slate-500">No indicators yet — these follow your recorded data.</p>
        ) : null}
        <div className="mt-6 grid gap-6 md:grid-cols-3">
          {circularStats.map((item) => (
            <div key={item.label} className="rounded-[1.5rem] border border-slate-200 bg-slate-50 p-5">
              <CircularProgress value={item.value} label={item.label} />
            </div>
          ))}
        </div>
      </motion.section>

    </div>
  );
}
