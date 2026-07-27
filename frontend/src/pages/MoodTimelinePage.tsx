import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import { Button } from '../components/ui/button';
import { useLogMood, useRecentMoods, type MoodEntry } from '../lib/mood';
import { ApiError } from '../lib/api';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

const moodChoices = [
  { label: 'Overwhelmed', score: 25 },
  { label: 'Low', score: 45 },
  { label: 'Balanced', score: 65 },
  { label: 'Calm', score: 80 },
  { label: 'Energized', score: 92 },
];

const dateLabel = new Intl.DateTimeFormat('en-US', { month: 'short', day: '2-digit' });
const weekdayLabel = new Intl.DateTimeFormat('en-US', { weekday: 'short' });

const lineOptions = {
  plugins: { legend: { display: false } },
  scales: { y: { beginAtZero: true, max: 100 } },
} as const;

function buildChartData(label: string, labels: string[], data: number[], color: string, fillColor: string) {
  return {
    labels,
    datasets: [
      {
        label,
        data,
        borderColor: color,
        backgroundColor: fillColor,
        tension: 0.35,
        fill: true,
        pointRadius: 4,
        pointHoverRadius: 6,
      },
    ],
  };
}

function averageByDay(entries: MoodEntry[], days: number) {
  const buckets: { key: string; label: string; scores: number[] }[] = [];
  const now = new Date();
  for (let i = days - 1; i >= 0; i -= 1) {
    const date = new Date(now);
    date.setDate(now.getDate() - i);
    buckets.push({ key: date.toDateString(), label: weekdayLabel.format(date), scores: [] });
  }
  const index = new Map(buckets.map((bucket) => [bucket.key, bucket]));
  entries.forEach((entry) => {
    const bucket = index.get(new Date(entry.recordedAt).toDateString());
    if (bucket) bucket.scores.push(entry.moodScore);
  });
  return buckets.map((bucket) => ({
    label: bucket.label,
    score: bucket.scores.length
      ? Math.round(bucket.scores.reduce((sum, value) => sum + value, 0) / bucket.scores.length)
      : 0,
  }));
}

export function MoodTimelinePage() {
  const { data: entries = [], isLoading } = useRecentMoods(30);
  const logMood = useLogMood();
  const [selected, setSelected] = useState(moodChoices[2]);
  const [note, setNote] = useState('');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const daily = useMemo(() => averageByDay(entries, 7), [entries]);
  const monthly = useMemo(() => averageByDay(entries, 30), [entries]);

  const dailyMoodData = buildChartData(
    'Daily mood',
    daily.map((d) => d.label),
    daily.map((d) => d.score),
    '#6366f1',
    'rgba(99, 102, 241, 0.15)',
  );

  const monthlyMoodData = buildChartData(
    'Monthly mood',
    monthly.map((_, i) => `Day ${i + 1}`),
    monthly.map((d) => d.score),
    '#10b981',
    'rgba(16, 185, 129, 0.15)',
  );

  const calendarDays = daily.map((d) => ({ day: d.label, score: d.score }));
  const historyItems = entries.slice(0, 8);

  const trendCards = useMemo(() => {
    if (entries.length === 0) {
      return [{ title: 'Getting started', value: '—', detail: 'Log a few moods to reveal your personal trends.' }];
    }
    const scores = entries.map((entry) => entry.moodScore);
    const average = Math.round(scores.reduce((sum, value) => sum + value, 0) / scores.length);
    const highest = Math.max(...scores);
    return [
      { title: 'Average mood', value: `${average}`, detail: 'Mean mood score across your recent entries.' },
      { title: 'Entries logged', value: `${entries.length}`, detail: 'Total mood check-ins in the last 30 days.' },
      { title: 'Best moment', value: `${highest}`, detail: 'Your highest mood score in this window.' },
    ];
  }, [entries]);

  const handleLog = async () => {
    setFeedback(null);
    try {
      await logMood.mutateAsync({ moodScore: selected.score, moodLabel: selected.label, note: note.trim() || undefined });
      setNote('');
      setFeedback({ type: 'success', text: 'Mood logged for today.' });
    } catch (error) {
      const text = error instanceof ApiError ? error.message : 'Unable to log your mood. Please try again.';
      setFeedback({ type: 'error', text });
    }
  };

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
      >
        <div className="max-w-3xl">
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Mood Timeline</p>
          <h1 className="mt-2 text-3xl font-semibold">A visual journey of your emotional rhythm</h1>
          <p className="mt-3 text-sm leading-7 text-slate-300">
            Track how you feel over time. Log today’s mood and watch your daily and monthly rhythm take shape.
          </p>
        </div>
      </motion.section>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.03 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Log Mood</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900">How are you feeling right now?</h2>
        <div className="mt-5 flex flex-wrap gap-2">
          {moodChoices.map((choice) => (
            <button
              key={choice.label}
              type="button"
              onClick={() => setSelected(choice)}
              className={`rounded-full border px-4 py-2 text-sm transition ${
                selected.label === choice.label
                  ? 'border-indigo-500 bg-indigo-600 text-white'
                  : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
              }`}
            >
              {choice.label}
            </button>
          ))}
        </div>
        <textarea
          value={note}
          onChange={(event) => setNote(event.target.value)}
          rows={3}
          placeholder="Add an optional note about your mood…"
          className="mt-4 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
        />
        <div className="mt-4 flex items-center gap-3">
          <Button type="button" onClick={handleLog} disabled={logMood.isPending}>
            {logMood.isPending ? 'Saving…' : 'Log mood'}
          </Button>
          {feedback ? (
            <span className={`text-sm ${feedback.type === 'success' ? 'text-emerald-600' : 'text-rose-600'}`}>
              {feedback.text}
            </span>
          ) : null}
        </div>
      </motion.section>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.05 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Daily Mood</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">This week’s emotional flow</h2>
          <div className="mt-6">
            <Line data={dailyMoodData} options={lineOptions} />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Weekly Mood</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">A broader perspective</h2>
          <div className="mt-6">
            <Line data={monthlyMoodData} options={lineOptions} />
          </div>
        </motion.section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.1 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Monthly Mood</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Longer-term trends</h2>
          <div className="mt-6">
            <Line data={monthlyMoodData} options={lineOptions} />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.12 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Calendar View</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Mood by day</h2>
          <div className="mt-6 grid gap-3 sm:grid-cols-7">
            {calendarDays.map((day) => (
              <div key={day.day} className="rounded-2xl border border-slate-200 bg-slate-50 p-3 text-center">
                <p className="text-sm font-medium text-slate-600">{day.day}</p>
                <div className="mt-3 h-16 rounded-xl bg-gradient-to-t from-indigo-500 to-cyan-400" style={{ opacity: 0.45 + day.score / 250 }} />
                <p className="mt-2 text-sm font-semibold text-slate-900">{day.score}</p>
              </div>
            ))}
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
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Mood History</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Recent reflections</h2>
          <div className="mt-6 space-y-3">
            {isLoading ? (
              <p className="text-sm text-slate-500">Loading your mood history…</p>
            ) : historyItems.length > 0 ? (
              historyItems.map((item) => (
                <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-semibold text-slate-900">{dateLabel.format(new Date(item.recordedAt))}</p>
                    <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600 shadow-sm">
                      {item.moodLabel ?? `${item.moodScore}`}
                    </span>
                  </div>
                  {item.note ? <p className="mt-2 text-sm leading-6 text-slate-600">{item.note}</p> : null}
                </div>
              ))
            ) : (
              <p className="text-sm text-slate-500">No mood entries yet. Log your first mood above.</p>
            )}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.16 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Wellness Trends</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Signals worth noticing</h2>
          <div className="mt-6 space-y-4">
            {trendCards.map((card) => (
              <div key={card.title} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-center justify-between gap-3">
                  <p className="font-semibold text-slate-900">{card.title}</p>
                  <span className="text-sm font-semibold text-emerald-600">{card.value}</span>
                </div>
                <p className="mt-2 text-sm leading-6 text-slate-600">{card.detail}</p>
              </div>
            ))}
          </div>
        </motion.section>
      </div>
    </div>
  );
}
