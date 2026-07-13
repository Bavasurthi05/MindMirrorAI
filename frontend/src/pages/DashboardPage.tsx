import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';

const quickActions = [
  { title: 'New journal entry', to: '/journal', icon: '📝' },
  { title: 'Run questionnaire', to: '/questionnaire', icon: '🧪' },
  { title: 'View mirror', to: '/mirror', icon: '🪞' },
  { title: 'Open reports', to: '/reports', icon: '📊' },
];

const recommendations = [
  {
    title: 'Mindful breathing',
    detail: 'A 5-minute reset to ease tension after a long stretch of focus.',
  },
  {
    title: 'Sleep support ritual',
    detail: 'A calming evening routine to improve consistency and rest quality.',
  },
  {
    title: 'Social connection',
    detail: 'A gentle reminder to reach out to one trusted person this week.',
  },
];

const triggerSummary = [
  { label: 'Workload pressure', value: 'High' },
  { label: 'Sleep disruption', value: 'Medium' },
  { label: 'Social fatigue', value: 'Low' },
];

const progressItems = [
  { label: 'Mood stability', progress: 82 },
  { label: 'Reflection consistency', progress: 74 },
  { label: 'Recovery habits', progress: 68 },
];

export function DashboardPage() {
  return (
    <div className="space-y-6">
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
              <h1 className="mt-2 text-3xl font-semibold">Alicia, your wellness outlook looks steady.</h1>
              <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-300">
                Your latest reflections suggest a balanced rhythm with a few opportunities to nurture rest and clarity.
              </p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 text-center backdrop-blur">
              <p className="text-3xl font-semibold">82</p>
              <p className="text-sm text-slate-300">Wellness score</p>
            </div>
          </div>
        </motion.section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Overall Wellness Score</p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-900">Balanced and improving</h2>
            </div>
            <div className="rounded-full bg-emerald-100 px-3 py-1 text-sm font-semibold text-emerald-700">+6%</div>
          </div>
          <div className="mt-6 rounded-2xl bg-slate-50 p-5">
            <div className="flex items-end justify-between">
              <div>
                <p className="text-4xl font-semibold text-slate-900">78/100</p>
                <p className="mt-2 text-sm text-slate-600">A calm, resilient profile with encouraging momentum.</p>
              </div>
              <div className="text-right text-sm text-slate-500">
                <p>Last updated</p>
                <p className="font-semibold text-slate-800">2h ago</p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Mental Health Summary</p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-900">Your week feels grounded and adaptive.</h2>
            </div>
          </div>
          <div className="mt-6 grid gap-4 md:grid-cols-3">
            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Stress</p>
              <p className="mt-1 text-xl font-semibold text-slate-900">Low</p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Energy</p>
              <p className="mt-1 text-xl font-semibold text-slate-900">Moderate</p>
            </div>
            <div className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">Reflection</p>
              <p className="mt-1 text-xl font-semibold text-slate-900">Consistent</p>
            </div>
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Recent Analysis</p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-900">New patterns detected</h2>
            </div>
          </div>
          <div className="mt-6 space-y-3">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-900">Tuesday reflection marked a calmer mood.</p>
              <p className="mt-1 text-sm text-slate-600">Mood trend improved following a steady evening routine.</p>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-900">Workload intensity increased slightly.</p>
              <p className="mt-1 text-sm text-slate-600">A short reset could improve focus and reduce emotional strain.</p>
            </div>
          </div>
        </section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Quick Actions</p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-900">Jump into your next step</h2>
            </div>
          </div>
          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            {quickActions.map((action) => (
              <Link key={action.title} to={action.to} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 transition hover:border-indigo-300 hover:bg-indigo-50">
                <div className="text-2xl">{action.icon}</div>
                <p className="mt-3 font-semibold text-slate-900">{action.title}</p>
              </Link>
            ))}
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Mood Overview</p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-900">A gentle view of your emotional rhythm</h2>
            </div>
          </div>
          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day, index) => (
              <div key={day} className="rounded-2xl bg-slate-50 p-4 text-center">
                <p className="text-sm text-slate-500">{day}</p>
                <div className="mt-3 h-20 rounded-xl bg-gradient-to-t from-indigo-500 to-cyan-400" style={{ opacity: 0.6 + index * 0.04 }} />
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Recommendation Cards</p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-900">Supportive ideas for today</h2>
            </div>
          </div>
          <div className="mt-6 space-y-3">
            {recommendations.map((item) => (
              <div key={item.title} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="font-semibold text-slate-900">{item.title}</p>
                <p className="mt-1 text-sm text-slate-600">{item.detail}</p>
              </div>
            ))}
          </div>
        </section>

        <div className="space-y-6">
          <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Weekly Insight Card</p>
            <h2 className="mt-2 text-2xl font-semibold text-slate-900">You’re showing stronger recovery habits.</h2>
            <p className="mt-3 text-sm leading-7 text-slate-600">
              Your latest weekly pattern suggests improved consistency in rest, reflection, and emotional steadiness.
            </p>
          </section>

          <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Trigger Summary</p>
            <div className="mt-6 space-y-3">
              {triggerSummary.map((item) => (
                <div key={item.label} className="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3">
                  <span className="text-sm text-slate-700">{item.label}</span>
                  <span className="text-sm font-semibold text-slate-900">{item.value}</span>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>

      <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Wellness Progress</p>
            <h2 className="mt-2 text-2xl font-semibold text-slate-900">Momentum across key habits</h2>
          </div>
        </div>
        <div className="mt-6 space-y-4">
          {progressItems.map((item) => (
            <div key={item.label}>
              <div className="mb-2 flex items-center justify-between text-sm text-slate-600">
                <span>{item.label}</span>
                <span className="font-semibold text-slate-900">{item.progress}%</span>
              </div>
              <div className="h-2 rounded-full bg-slate-200">
                <div className="h-2 rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500" style={{ width: `${item.progress}%` }} />
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
