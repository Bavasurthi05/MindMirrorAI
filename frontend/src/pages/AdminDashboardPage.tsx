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
import { Line, Bar, Doughnut } from 'react-chartjs-2';
import { useAdminOverview } from '../lib/admin';

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

const weeklyReportsData = {
  labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  datasets: [
    {
      label: 'Weekly reports',
      data: [24, 31, 27, 35, 29, 41, 33],
      backgroundColor: ['#818cf8', '#60a5fa', '#22d3ee', '#34d399', '#f59e0b', '#fb7185', '#a78bfa'],
      borderRadius: 10,
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

const weeklyReports = [
  { title: 'Weekly wellness report', count: '142 generated' },
  { title: 'Stress trend summary', count: '89 generated' },
  { title: 'Recovery recommendations', count: '66 generated' },
];

export function AdminDashboardPage() {
  const { data: overview } = useAdminOverview();

  const analyticsCards = overview
    ? [
        { label: 'Total Users', value: `${overview.totalUsers}`, detail: `${overview.verifiedUsers} verified` },
        { label: 'Journal Entries', value: `${overview.totalJournalEntries}`, detail: 'Across all users' },
        { label: 'Mood Check-ins', value: `${overview.totalMoodEntries}`, detail: 'Logged moods' },
        { label: 'Assessments', value: `${overview.totalAssessments}`, detail: 'Completed questionnaires' },
      ]
    : analyticsCardsFallback;

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
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Trigger Analytics</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Most common trigger categories</h2>
          <div className="mt-6">
            <Bar data={weeklyReportsData} options={{ plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }} />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.12 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Weekly Reports</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Recent report outputs</h2>
          <div className="mt-6 space-y-3">
            {weeklyReports.map((item) => (
              <div key={item.title} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-center justify-between gap-3">
                  <p className="font-semibold text-slate-900">{item.title}</p>
                  <span className="text-sm text-slate-500">{item.count}</span>
                </div>
              </div>
            ))}
          </div>
        </motion.section>
      </div>
    </div>
  );
}
