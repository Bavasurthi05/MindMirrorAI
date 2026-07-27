import { motion } from 'framer-motion';
import { useWeeklyInsights } from '../lib/analytics';

export function WeeklyInsightsPage() {
  const { data, isLoading, isError } = useWeeklyInsights();

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
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Weekly Insights</p>
            <h1 className="mt-2 text-3xl font-semibold">Your week at a glance</h1>
            <p className="mt-3 text-sm leading-7 text-slate-300">
              AI-generated highlights from your mood check-ins, journals, and triggers over the past week.
            </p>
          </div>
          {data ? (
            <div className="rounded-2xl border border-white/10 bg-white/5 px-6 py-4 text-center">
              <p className="text-4xl font-semibold">{data.wellbeingIndex}</p>
              <p className="text-xs text-slate-300">Wellbeing index</p>
            </div>
          ) : null}
        </div>
      </motion.section>

      {isLoading ? (
        <p className="text-sm text-slate-500">Loading insights…</p>
      ) : isError || !data ? (
        <p className="text-sm text-slate-500">
          Insights unavailable. Log some moods and journals this week to generate them.
        </p>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1.4fr_0.6fr]">
          <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Highlights</p>
            <ul className="mt-4 space-y-3">
              {data.highlights.map((highlight, index) => (
                <li key={index} className="flex items-start gap-3 text-sm leading-6 text-slate-700">
                  <span className="mt-1 h-2 w-2 flex-shrink-0 rounded-full bg-indigo-500" />
                  {highlight}
                </li>
              ))}
            </ul>
          </section>

          <section className="rounded-[2rem] border border-indigo-100 bg-indigo-50/60 p-8 shadow-sm">
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-indigo-600">Focus area</p>
            <p className="mt-3 text-xl font-semibold text-slate-900">{data.focusArea}</p>
          </section>
        </div>
      )}
    </div>
  );
}
