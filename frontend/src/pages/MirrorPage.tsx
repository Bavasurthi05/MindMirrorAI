import { motion } from 'framer-motion';
import { useAnalyticsOverview } from '../lib/analytics';

const RADAR_LABELS = ['Stress', 'Confidence', 'Sleep', 'Motivation', 'Social', 'Happiness'];
const DEFAULT_RADAR = [82, 68, 74, 79, 88, 71];

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
  const { data } = useAnalyticsOverview();

  const radarByLabel = new Map((data?.radar ?? []).map((m) => [m.label, m.value]));
  const radarValues = RADAR_LABELS.map((label, index) =>
    radarByLabel.has(label) ? (radarByLabel.get(label) as number) : DEFAULT_RADAR[index],
  );
  const overallWellness = data?.overallWellness ?? 82;

  const metrics = [
    { label: 'Wellness Score', value: `${overallWellness}%`, tone: 'from-cyan-500 to-indigo-500' },
    ...RADAR_LABELS.map((label, index) => ({
      label,
      value: `${radarValues[index]}%`,
      tone: 'from-indigo-500 to-violet-500',
    })),
  ];

  const circularStats = [
    { label: 'Happiness', value: Math.round(radarByLabel.get('Happiness') ?? DEFAULT_RADAR[5]) },
    { label: 'Confidence', value: Math.round(radarByLabel.get('Confidence') ?? DEFAULT_RADAR[1]) },
    { label: 'Motivation', value: Math.round(radarByLabel.get('Motivation') ?? DEFAULT_RADAR[3]) },
  ];

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
            <p className="text-4xl font-semibold">{overallWellness}</p>
            <p className="text-sm text-slate-300">Overall wellness</p>
          </div>
        </div>
      </motion.section>

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
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Wellness Snapshot</p>
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
        <div className="mt-6 grid gap-6 md:grid-cols-3">
          {circularStats.map((item) => (
            <div key={item.label} className="rounded-[1.5rem] border border-slate-200 bg-slate-50 p-5">
              <CircularProgress value={item.value} label={item.label} />
            </div>
          ))}
        </div>
      </motion.section>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.16 }}
        className="rounded-[2rem] border border-slate-200 bg-slate-50 p-8 shadow-sm"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Animated Cards</p>
        <div className="mt-6 grid gap-4 lg:grid-cols-3">
          {[
            { title: 'Growth Pattern', body: 'Signals show gentle improvement in consistency and resilience.' },
            { title: 'Reflection Focus', body: 'Your mood appears more grounded after intentional rest periods.' },
            { title: 'Social Energy', body: 'Interaction seems balanced, with space for deeper connection.' },
          ].map((card) => (
            <motion.div
              key={card.title}
              whileHover={{ y: -4, scale: 1.01 }}
              className="rounded-[1.4rem] border border-slate-200 bg-white p-5 shadow-sm"
            >
              <h3 className="text-lg font-semibold text-slate-900">{card.title}</h3>
              <p className="mt-2 text-sm leading-7 text-slate-600">{card.body}</p>
            </motion.div>
          ))}
        </div>
      </motion.section>
    </div>
  );
}
