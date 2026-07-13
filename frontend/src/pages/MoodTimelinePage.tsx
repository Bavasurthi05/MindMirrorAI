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

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

const dailyMoodData = {
  labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  datasets: [
    {
      label: 'Daily mood',
      data: [72, 68, 76, 81, 78, 84, 80],
      borderColor: '#6366f1',
      backgroundColor: 'rgba(99, 102, 241, 0.15)',
      tension: 0.35,
      fill: true,
      pointRadius: 4,
      pointHoverRadius: 6,
    },
  ],
};

const weeklyMoodData = {
  labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
  datasets: [
    {
      label: 'Weekly mood',
      data: [70, 74, 78, 82],
      borderColor: '#22d3ee',
      backgroundColor: 'rgba(34, 211, 238, 0.15)',
      tension: 0.35,
      fill: true,
      pointRadius: 4,
      pointHoverRadius: 6,
    },
  ],
};

const monthlyMoodData = {
  labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
  datasets: [
    {
      label: 'Monthly mood',
      data: [65, 69, 72, 74, 78, 81],
      borderColor: '#10b981',
      backgroundColor: 'rgba(16, 185, 129, 0.15)',
      tension: 0.35,
      fill: true,
      pointRadius: 4,
      pointHoverRadius: 6,
    },
  ],
};

const historyItems = [
  { date: 'Jul 12', mood: 'Calm', note: 'Rest felt steady and grounded.' },
  { date: 'Jul 11', mood: 'Reflective', note: 'A quieter day helped with clarity.' },
  { date: 'Jul 10', mood: 'Energized', note: 'Morning energy stayed high through the afternoon.' },
  { date: 'Jul 09', mood: 'Balanced', note: 'A good rhythm between work and rest.' },
];

const calendarDays = [
  { day: 'Mon', score: 72 },
  { day: 'Tue', score: 68 },
  { day: 'Wed', score: 78 },
  { day: 'Thu', score: 82 },
  { day: 'Fri', score: 76 },
  { day: 'Sat', score: 84 },
  { day: 'Sun', score: 80 },
];

const trendCards = [
  { title: 'Steadiness', value: '+8%', detail: 'Mood has been more stable over the last 3 weeks.' },
  { title: 'Rest quality', value: '+5%', detail: 'Sleep rhythm appears more intentional and consistent.' },
  { title: 'Energy', value: '+6%', detail: 'Motivation remains steady across the week.' },
];

export function MoodTimelinePage() {
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
            This mock timeline page brings together daily, weekly, and monthly mood patterns in a polished, supportive experience.
          </p>
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
            <Line data={dailyMoodData} options={{ plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } }} />
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
            <Line data={weeklyMoodData} options={{ plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } }} />
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
            <Line data={monthlyMoodData} options={{ plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } }} />
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
            {historyItems.map((item) => (
              <div key={item.date} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-center justify-between gap-3">
                  <p className="font-semibold text-slate-900">{item.date}</p>
                  <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600 shadow-sm">{item.mood}</span>
                </div>
                <p className="mt-2 text-sm leading-6 text-slate-600">{item.note}</p>
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
