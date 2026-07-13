import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';

const summaryCards = [
  { label: 'Overall Wellness', value: '82/100' },
  { label: 'Stress Trend', value: 'Low' },
  { label: 'Recovery Score', value: '78%' },
];

const predictionSummary = [
  'Mood pattern suggests a steady week with moderate energy.',
  'Supportive habits appear to be improving overall resilience.',
  'A small increase in rest consistency could strengthen calmness further.',
];

const triggerAnalysis = [
  { title: 'Workload pressure', strength: 'High' },
  { title: 'Sleep disruption', strength: 'Medium' },
  { title: 'Social fatigue', strength: 'Low' },
];

const recommendations = [
  'Continue the current journaling routine.',
  'Add one short breathwork session after work hours.',
  'Maintain a consistent evening wind-down ritual.',
];

export function ReportPreviewPage() {
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
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">PDF Report Preview</p>
            <h1 className="mt-2 text-3xl font-semibold text-slate-900">Preview your wellness report</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">
              This page presents a polished, placeholder report layout for future export workflows.
            </p>
          </div>
          <Button disabled className="w-full sm:w-auto">
            Download Report
          </Button>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {summaryCards.map((card) => (
            <div key={card.label} className="rounded-[1.4rem] border border-slate-200 bg-slate-50 p-5">
              <p className="text-sm text-slate-500">{card.label}</p>
              <p className="mt-2 text-xl font-semibold text-slate-900">{card.value}</p>
            </div>
          ))}
        </div>
      </motion.section>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.05 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Wellness Summary</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">What your current snapshot says</h2>
          <div className="mt-6 rounded-[1.4rem] border border-slate-200 bg-slate-50 p-5 text-sm leading-7 text-slate-600">
            Your recent reflections suggest a thoughtful balance between calm, motivation, and emotional awareness. The report preview highlights steady progress without overstating certainty.
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Prediction Summary</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Short-term outlook</h2>
          <ul className="mt-6 space-y-3 text-sm leading-7 text-slate-600">
            {predictionSummary.map((item) => (
              <li key={item} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                {item}
              </li>
            ))}
          </ul>
        </motion.section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.1 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Trigger Analysis</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Most relevant pressure points</h2>
          <div className="mt-6 space-y-3">
            {triggerAnalysis.map((item) => (
              <div key={item.title} className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <span className="text-sm font-medium text-slate-800">{item.title}</span>
                <span className="text-sm font-semibold text-indigo-600">{item.strength}</span>
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
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Charts</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Visual snapshot</h2>
          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            <div className="rounded-[1.4rem] border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-900">Mood trend</p>
              <div className="mt-3 h-20 rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-500" />
            </div>
            <div className="rounded-[1.4rem] border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-900">Trigger intensity</p>
              <div className="mt-3 h-20 rounded-xl bg-gradient-to-r from-amber-400 to-rose-500" />
            </div>
          </div>
        </motion.section>
      </div>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.14 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Recommendations</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900">Suggested next steps</h2>
        <div className="mt-6 space-y-3">
          {recommendations.map((item) => (
            <div key={item} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-7 text-slate-600">
              {item}
            </div>
          ))}
        </div>
      </motion.section>
    </div>
  );
}
