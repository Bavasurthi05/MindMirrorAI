import { motion } from 'framer-motion';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  RadialLinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';
import { Radar, Line, Bar, Pie } from 'react-chartjs-2';
import { useAnalyticsOverview } from '../lib/analytics';
import { HeatmapCalendar } from '../components/charts/HeatmapCalendar';
import { EmotionTimeline } from '../components/charts/EmotionTimeline';

ChartJS.register(
  CategoryScale,
  LinearScale,
  RadialLinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Filler,
  Tooltip,
  Legend,
);

const PIE_COLORS = ['#6366f1', '#22d3ee', '#f59e0b', '#10b981', '#fb7185', '#a78bfa', '#38bdf8'];

function ProgressRing({ value }: { value: number }) {
  const radius = 52;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;
  return (
    <div className="relative flex h-40 w-40 items-center justify-center">
      <svg viewBox="0 0 130 130" className="h-40 w-40 -rotate-90">
        <circle cx="65" cy="65" r={radius} stroke="#e2e8f0" strokeWidth="12" fill="none" />
        <circle
          cx="65"
          cy="65"
          r={radius}
          stroke="url(#ringGradient)"
          strokeWidth="12"
          strokeLinecap="round"
          fill="none"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
        <defs>
          <linearGradient id="ringGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#22d3ee" />
            <stop offset="100%" stopColor="#6366f1" />
          </linearGradient>
        </defs>
      </svg>
      <div className="absolute text-center">
        <p className="text-3xl font-semibold text-slate-900">{value}%</p>
        <p className="text-xs text-slate-500">Wellness</p>
      </div>
    </div>
  );
}

function Panel({ title, subtitle, children }: { title: string; subtitle?: string; children: React.ReactNode }) {
  return (
    <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">{title}</p>
      {subtitle ? <p className="mt-1 text-sm text-slate-500">{subtitle}</p> : null}
      <div className="mt-4">{children}</div>
    </section>
  );
}

export function AnalyticsPage() {
  const { data, isLoading, isError } = useAnalyticsOverview();

  if (isLoading) {
    return <p className="text-sm text-slate-500">Loading analytics…</p>;
  }

  if (isError || !data) {
    return (
      <p className="text-sm text-slate-500">
        Analytics unavailable. Log some moods, triggers, and assessments to populate this view.
      </p>
    );
  }

  const radarData = {
    labels: data.radar.map((m) => m.label),
    datasets: [
      {
        label: 'Wellbeing profile',
        data: data.radar.map((m) => m.value),
        backgroundColor: 'rgba(99, 102, 241, 0.2)',
        borderColor: '#6366f1',
        borderWidth: 2,
        pointBackgroundColor: '#6366f1',
      },
    ],
  };

  const moodLineData = {
    labels: data.moodSeries.map((p) => p.date.slice(5)),
    datasets: [
      {
        label: 'Mood',
        data: data.moodSeries.map((p) => p.score),
        borderColor: '#22d3ee',
        backgroundColor: 'rgba(34, 211, 238, 0.15)',
        tension: 0.35,
        fill: true,
      },
    ],
  };

  const weeklyTrendData = {
    labels: data.weeklyTrend.map((m) => m.label),
    datasets: [
      {
        label: 'Avg mood',
        data: data.weeklyTrend.map((m) => m.value),
        backgroundColor: ['#818cf8', '#60a5fa', '#22d3ee', '#34d399'],
        borderRadius: 10,
      },
    ],
  };

  const emotionPieData = {
    labels: data.emotionDistribution.map((m) => m.label),
    datasets: [
      {
        data: data.emotionDistribution.map((m) => m.value),
        backgroundColor: PIE_COLORS,
        borderWidth: 0,
      },
    ],
  };

  const triggerBarData = {
    labels: data.triggerDistribution.map((m) => m.label),
    datasets: [
      {
        label: 'Intensity',
        data: data.triggerDistribution.map((m) => m.value),
        backgroundColor: '#f59e0b',
        borderRadius: 8,
      },
    ],
  };

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
      >
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="max-w-2xl">
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Analytics Dashboard</p>
            <h1 className="mt-2 text-3xl font-semibold">Your wellbeing, visualized</h1>
            <p className="mt-3 text-sm leading-7 text-slate-300">
              Radar profile, daily heatmap, emotion timeline, and trends — all derived from your real check-ins,
              journals, triggers, and assessments.
            </p>
          </div>
          <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
            <ProgressRing value={data.overallWellness} />
          </div>
        </div>
      </motion.section>

      <div className="grid gap-6 xl:grid-cols-2">
        <Panel title="Radar Profile" subtitle="Stress • Confidence • Sleep • Motivation • Social • Happiness">
          <Radar
            data={radarData}
            options={{ scales: { r: { suggestedMin: 0, suggestedMax: 100 } }, plugins: { legend: { display: false } } }}
          />
        </Panel>

        <Panel title="Wellness Heatmap" subtitle="Last 35 days">
          <HeatmapCalendar cells={data.heatmap} />
        </Panel>
      </div>

      <Panel title="Emotion Timeline" subtitle="Most recent check-ins">
        <EmotionTimeline points={data.emotionTimeline} />
      </Panel>

      <div className="grid gap-6 xl:grid-cols-2">
        <Panel title="Mood Trend" subtitle="Last 14 days">
          <Line data={moodLineData} options={{ plugins: { legend: { display: false } }, scales: { y: { min: 0, max: 100 } } }} />
        </Panel>

        <Panel title="Weekly Trend" subtitle="Average mood per week">
          <Bar data={weeklyTrendData} options={{ plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } }} />
        </Panel>

        <Panel title="Emotion Distribution">
          {data.emotionDistribution.length > 0 ? (
            <Pie data={emotionPieData} options={{ plugins: { legend: { position: 'bottom' } } }} />
          ) : (
            <p className="text-sm text-slate-500">No emotion data yet.</p>
          )}
        </Panel>

        <Panel title="Trigger Intensity" subtitle="By category (0–100)">
          {data.triggerDistribution.length > 0 ? (
            <Bar data={triggerBarData} options={{ plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } }} />
          ) : (
            <p className="text-sm text-slate-500">No triggers logged yet.</p>
          )}
        </Panel>
      </div>
    </div>
  );
}
