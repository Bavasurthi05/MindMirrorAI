import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  PointElement,
  LineElement,
  RadialLinearScale,
} from 'chart.js';
import { Pie, Bar } from 'react-chartjs-2';
import { Button } from '../components/ui/button';
import { useLogTrigger, useTriggerAnalytics, useTriggers } from '../lib/triggers';
import { ApiError } from '../lib/api';

ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  PointElement,
  LineElement,
  RadialLinearScale,
);

const piePalette = ['#6366f1', '#22d3ee', '#f59e0b', '#10b981', '#fb7185', '#a78bfa', '#60a5fa'];

const barData = {
  labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  datasets: [
    {
      label: 'Trigger intensity',
      data: [4, 6, 5, 7, 3, 8, 5],
      backgroundColor: ['#818cf8', '#60a5fa', '#22d3ee', '#34d399', '#f59e0b', '#fb7185', '#a78bfa'],
      borderRadius: 10,
    },
  ],
};

const timelineItems = [
  { day: 'Monday', time: '08:30', event: 'Heavy workload affected focus and calm.' },
  { day: 'Wednesday', time: '21:00', event: 'Poor sleep quality increased emotional strain.' },
  { day: 'Friday', time: '18:15', event: 'Social fatigue became more noticeable after back-to-back meetings.' },
];

const heatMapRows = [
  { label: 'Morning', values: [1, 3, 2, 1, 2, 3, 2] },
  { label: 'Midday', values: [4, 5, 3, 4, 3, 5, 4] },
  { label: 'Evening', values: [2, 4, 3, 2, 3, 4, 3] },
  { label: 'Night', values: [3, 2, 2, 3, 2, 4, 3] },
];

const ranking = [
  { label: 'Workload pressure', score: '9.2/10' },
  { label: 'Sleep disruption', score: '7.8/10' },
  { label: 'Social fatigue', score: '6.4/10' },
  { label: 'Routine drift', score: '5.9/10' },
];

function getHeatCellClass(value: number) {
  if (value >= 4) return 'bg-rose-500';
  if (value === 3) return 'bg-amber-400';
  if (value === 2) return 'bg-sky-400';
  return 'bg-emerald-400';
}

const relativeTime = (iso: string) => {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr ago`;
  const days = Math.round(hours / 24);
  return `${days} d ago`;
};

const triggerCategories = ['Workload', 'Sleep', 'Social', 'Routine', 'Health', 'Finance'];

export function TriggerAnalyticsPage() {
  const { data: analytics } = useTriggerAnalytics();
  const { data: triggers = [] } = useTriggers();
  const logTrigger = useLogTrigger();
  const [category, setCategory] = useState(triggerCategories[0]);
  const [intensity, setIntensity] = useState(5);
  const [note, setNote] = useState('');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const categories = analytics?.categories ?? [];

  const triggerCards = categories.slice(0, 3).map((stat) => ({
    title: stat.category,
    value: `${stat.averageIntensity.toFixed(1)}/10`,
    detail: `${stat.count} logged occurrence${stat.count === 1 ? '' : 's'}.`,
  }));

  const pieData = useMemo(
    () => ({
      labels: categories.map((stat) => stat.category),
      datasets: [
        {
          data: categories.map((stat) => stat.count),
          backgroundColor: categories.map((_, i) => piePalette[i % piePalette.length]),
          borderWidth: 0,
        },
      ],
    }),
    [categories],
  );

  const recentTriggers = triggers.slice(0, 5).map((entry) => ({
    title: `${entry.category} · intensity ${entry.intensity}`,
    time: relativeTime(entry.occurredAt),
  }));

  const handleLog = async () => {
    setFeedback(null);
    try {
      await logTrigger.mutateAsync({ category, intensity, note: note.trim() || undefined });
      setNote('');
      setFeedback({ type: 'success', text: 'Trigger logged.' });
    } catch (error) {
      const text = error instanceof ApiError ? error.message : 'Unable to log trigger. Please try again.';
      setFeedback({ type: 'error', text });
    }
  };

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Trigger Analytics</p>
            <h1 className="mt-2 text-3xl font-semibold text-slate-900">A calm view of emotional pressure points</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">
              Track recurring stress triggers, log new ones, and see how they distribute across categories.
            </p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
            <p className="font-semibold text-slate-900">Total logged</p>
            <p>{analytics?.totalCount ?? 0} triggers · avg {analytics?.averageIntensity ?? 0}/10</p>
          </div>
        </div>

        <div className="mt-6 rounded-[1.5rem] border border-slate-200 bg-slate-50 p-5">
          <p className="text-sm font-semibold text-slate-900">Log a trigger</p>
          <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_auto_auto]">
            <select
              value={category}
              onChange={(event) => setCategory(event.target.value)}
              className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-indigo-500"
            >
              {triggerCategories.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600">
              <span>Intensity</span>
              <input
                type="range"
                min={1}
                max={10}
                value={intensity}
                onChange={(event) => setIntensity(Number(event.target.value))}
              />
              <span className="w-6 text-center font-semibold text-slate-900">{intensity}</span>
            </div>
            <Button type="button" onClick={handleLog} disabled={logTrigger.isPending}>
              {logTrigger.isPending ? 'Saving…' : 'Log'}
            </Button>
          </div>
          <input
            value={note}
            onChange={(event) => setNote(event.target.value)}
            placeholder="Optional note…"
            className="mt-3 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-indigo-500"
          />
          {feedback ? (
            <p className={`mt-3 text-sm ${feedback.type === 'success' ? 'text-emerald-600' : 'text-rose-600'}`}>
              {feedback.text}
            </p>
          ) : null}
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {triggerCards.length > 0 ? (
            triggerCards.map((card) => (
              <div key={card.title} className="rounded-[1.4rem] border border-slate-200 bg-slate-50 p-5">
                <p className="text-sm font-semibold text-slate-900">{card.title}</p>
                <p className="mt-2 text-xl font-semibold text-indigo-600">{card.value}</p>
                <p className="mt-2 text-sm leading-6 text-slate-600">{card.detail}</p>
              </div>
            ))
          ) : (
            <p className="text-sm text-slate-500">No triggers logged yet. Add one above to see analytics.</p>
          )}
        </div>
      </motion.section>

      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.05 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Pie Chart</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Trigger distribution</h2>
          <div className="mt-6 mx-auto max-w-sm">
            <Pie data={pieData} options={{ plugins: { legend: { position: 'bottom' } } }} />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Bar Chart</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Weekly trigger intensity</h2>
          <div className="mt-6">
            <Bar data={barData} options={{ scales: { y: { beginAtZero: true, ticks: { stepSize: 2 } } } }} />
          </div>
        </motion.section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.1 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Timeline</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Trigger events over time</h2>
          <div className="mt-6 space-y-4">
            {timelineItems.map((item) => (
              <div key={item.day + item.time} className="flex gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-100 text-sm font-semibold text-indigo-700">
                  {item.time.split(':')[0]}
                </div>
                <div>
                  <p className="font-semibold text-slate-900">{item.day}</p>
                  <p className="text-sm text-slate-500">{item.time}</p>
                  <p className="mt-2 text-sm leading-6 text-slate-600">{item.event}</p>
                </div>
              </div>
            ))}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.12 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Heat Map</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Intensity across the week</h2>
          <div className="mt-6 overflow-hidden rounded-[1.4rem] border border-slate-200">
            <div className="grid grid-cols-[90px_repeat(7,minmax(0,1fr))] bg-slate-50 text-sm text-slate-600">
              <div className="border-b border-slate-200 p-3 font-medium">Time</div>
              {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day) => (
                <div key={day} className="border-b border-slate-200 border-l border-slate-200 p-3 text-center font-medium">
                  {day}
                </div>
              ))}
              {heatMapRows.map((row) => (
                <>
                  <div key={row.label} className="border-b border-slate-200 p-3 font-medium text-slate-700">
                    {row.label}
                  </div>
                  {row.values.map((value, index) => (
                    <div key={`${row.label}-${index}`} className={`border-b border-l border-slate-200 p-3 ${getHeatCellClass(value)}`} />
                  ))}
                </>
              ))}
            </div>
          </div>
        </motion.section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.14 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Trigger Ranking</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Most influential triggers</h2>
          <div className="mt-6 space-y-3">
            {ranking.map((item) => (
              <div key={item.label} className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <span className="font-medium text-slate-800">{item.label}</span>
                <span className="text-sm font-semibold text-indigo-600">{item.score}</span>
              </div>
            ))}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.16 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Recent Triggers</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Latest signals</h2>
          <div className="mt-6 space-y-3">
            {recentTriggers.map((item) => (
              <div key={item.title} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-center justify-between gap-3">
                  <p className="font-semibold text-slate-900">{item.title}</p>
                  <span className="text-sm text-slate-500">{item.time}</span>
                </div>
              </div>
            ))}
          </div>
        </motion.section>
      </div>
    </div>
  );
}
