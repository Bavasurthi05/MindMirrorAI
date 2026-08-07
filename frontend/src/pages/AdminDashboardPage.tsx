import { motion } from 'framer-motion';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line, Doughnut } from 'react-chartjs-2';
import { useAdminFeedback, useAdminModelMetrics, useAdminOverview, useAdminUsers, useSetUserEnabled } from '../lib/admin';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend);

const analyticsCardsFallback = [
  { label: 'Total Users', value: '—', detail: 'Registered accounts' },
  { label: 'Verified Users', value: '—', detail: 'Confirmed emails' },
  { label: 'Journal Entries', value: '—', detail: 'Across all users' },
  { label: 'Assessments', value: '—', detail: 'Completed check-ins' },
];

const growthData = {
  labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul'],
  datasets: [
    {
      label: 'User growth',
      data: [1800, 2200, 2800, 3600, 4700, 6400, 8200],
      borderColor: '#6366f1',
      backgroundColor: 'rgba(99, 102, 241, 0.15)',
      tension: 0.35,
      fill: true,
    },
  ],
};

const triggerDistribution = {
  labels: ['Workload', 'Sleep', 'Social', 'Lifestyle'],
  datasets: [
    {
      data: [42, 24, 18, 16],
      backgroundColor: ['#6366f1', '#22d3ee', '#f59e0b', '#10b981'],
      borderWidth: 0,
    },
  ],
};

export function AdminDashboardPage() {
  const { data: overview } = useAdminOverview();
  const { data: users = [] } = useAdminUsers();
  const { data: feedback = [] } = useAdminFeedback();
  const { data: modelMetrics } = useAdminModelMetrics();
  const setUserEnabled = useSetUserEnabled();

  const analyticsCards = overview
    ? [
        { label: 'Total Users', value: `${overview.totalUsers}`, detail: `${overview.verifiedUsers} verified` },
        { label: 'Journal Entries', value: `${overview.totalJournalEntries}`, detail: 'Across all users' },
        { label: 'Mood Check-ins', value: `${overview.totalMoodEntries}`, detail: 'Logged moods' },
        { label: 'Assessments', value: `${overview.totalAssessments}`, detail: 'Completed questionnaires' },
      ]
    : analyticsCardsFallback;

  const modelRows = modelMetrics?.models
    ? Object.entries(modelMetrics.models).map(([key, value]) => ({ key, ...value }))
    : [];

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Admin Dashboard</p>
            <h1 className="mt-2 text-3xl font-semibold">Administrative oversight for the platform</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-300">
              This mock admin view highlights user growth, prediction volume, wellness insights, and trigger trends.
            </p>
          </div>
          <div className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 text-sm text-slate-200 backdrop-blur">
            <p className="font-semibold text-white">Snapshot</p>
            <p>Monitoring 24 active insight clusters</p>
          </div>
        </div>
      </motion.section>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {analyticsCards.map((card) => (
          <motion.div
            key={card.label}
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
            className="rounded-[1.4rem] border border-slate-200 bg-white p-5 shadow-sm"
          >
            <p className="text-sm text-slate-500">{card.label}</p>
            <p className="mt-3 text-2xl font-semibold text-slate-900">{card.value}</p>
            <p className="mt-2 text-sm text-slate-600">{card.detail}</p>
          </motion.div>
        ))}
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.05 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">User Growth</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Adoption over time</h2>
          <div className="mt-6">
            <Line data={growthData} options={{ plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }} />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Wellness Statistics</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Platform wellbeing health</h2>
          <div className="mt-6 mx-auto max-w-sm">
            <Doughnut data={triggerDistribution} options={{ plugins: { legend: { position: 'bottom' } } }} />
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
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">User management</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Accounts and access</h2>
          <div className="mt-6 space-y-3">
            {users.map((user) => (
              <div key={user.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-900">{user.fullName}</p>
                    <p className="text-sm text-slate-600">{user.email}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-slate-700">{user.role}</span>
                    <button
                      type="button"
                      onClick={() => setUserEnabled.mutate({ id: user.id, enabled: !user.enabled })}
                      className={`rounded-full px-3 py-1 text-sm font-semibold ${user.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}
                    >
                      {user.enabled ? 'Enabled' : 'Disabled'}
                    </button>
                  </div>
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
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Model accuracy</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Random Forest vs baseline</h2>
          <div className="mt-6 space-y-3">
            {modelRows.map((row) => (
              <div key={row.key} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-center justify-between">
                  <p className="font-semibold text-slate-900">{row.name}</p>
                  <span className="text-sm text-slate-600">{row.accuracy.toFixed(2)} accuracy</span>
                </div>
                <p className="mt-1 text-sm text-slate-600">F1 macro: {row.f1Macro.toFixed(2)}</p>
              </div>
            ))}
          </div>
        </motion.section>
      </div>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.14 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Feedback review</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900">Recent user submissions</h2>
        <div className="mt-6 space-y-3">
          {feedback.map((item) => (
            <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-slate-900">{item.userName}</p>
                  <p className="text-sm text-slate-600">{item.userEmail}</p>
                </div>
                <span className="rounded-full bg-amber-100 px-3 py-1 text-sm font-semibold text-amber-700">{item.rating}/5</span>
              </div>
              <p className="mt-3 text-sm leading-7 text-slate-700">{item.message}</p>
            </div>
          ))}
        </div>
      </motion.section>
    </div>
  );
}
