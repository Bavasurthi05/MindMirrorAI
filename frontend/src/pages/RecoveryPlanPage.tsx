import { motion } from 'framer-motion';
import { useRecoveryPlan, useToggleRecoveryAction } from '../lib/recovery';

const iconFor = (title: string) => {
  const map: Record<string, string> = {
    Meditation: '🧘',
    'Breathing Exercises': '🌬️',
    Journaling: '📝',
    Walking: '🚶',
    'Sleep Improvement': '🌙',
    'Music Therapy': '🎵',
    'Professional Consultation': '🩺',
    'Daily Check-in': '✅',
  };
  return map[title] ?? '💡';
};

export function RecoveryPlanPage() {
  const { data: recommendations = [], isLoading } = useRecoveryPlan();
  const toggleAction = useToggleRecoveryAction();

  const completed = recommendations.filter((item) => item.completed).length;
  const progress = recommendations.length ? Math.round((completed / recommendations.length) * 100) : 0;

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
      >
        <div className="max-w-3xl">
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Personalized Recovery Plan</p>
          <h1 className="mt-2 text-3xl font-semibold">A calm, guided plan for steadier wellbeing</h1>
          <p className="mt-3 text-sm leading-7 text-slate-300">
            Tailored to your latest check-in. Mark actions complete as you work through them.
          </p>
          <div className="mt-5">
            <div className="flex items-center justify-between text-sm text-slate-300">
              <span>Progress</span>
              <span>{completed} of {recommendations.length} complete</span>
            </div>
            <div className="mt-2 h-2 rounded-full bg-white/20">
              <div className="h-2 rounded-full bg-gradient-to-r from-cyan-400 to-indigo-400" style={{ width: `${progress}%` }} />
            </div>
          </div>
        </div>
      </motion.section>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.05 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        {isLoading ? (
          <p className="text-sm text-slate-500">Preparing your personalized plan…</p>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {recommendations.map((item) => (
              <motion.div
                key={item.id}
                whileHover={{ y: -4, scale: 1.01 }}
                className={`rounded-[1.4rem] border p-5 transition ${
                  item.completed ? 'border-emerald-300 bg-emerald-50' : 'border-slate-200 bg-slate-50'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-3xl">{iconFor(item.title)}</p>
                    <h2 className="mt-3 text-lg font-semibold text-slate-900">{item.title}</h2>
                  </div>
                  {item.duration ? (
                    <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600 shadow-sm">
                      {item.duration}
                    </span>
                  ) : null}
                </div>
                {item.focus ? <p className="mt-3 text-sm font-medium text-cyan-700">{item.focus}</p> : null}
                {item.description ? <p className="mt-2 text-sm leading-7 text-slate-600">{item.description}</p> : null}
                <button
                  type="button"
                  onClick={() => toggleAction.mutate(item.id)}
                  disabled={toggleAction.isPending}
                  className={`mt-4 w-full rounded-xl px-3 py-2 text-sm font-medium transition ${
                    item.completed
                      ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                      : 'bg-slate-900 text-white hover:bg-slate-800'
                  }`}
                >
                  {item.completed ? 'Completed ✓' : 'Mark complete'}
                </button>
              </motion.div>
            ))}
          </div>
        )}
      </motion.section>
    </div>
  );
}
