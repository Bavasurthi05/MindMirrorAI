import { motion } from 'framer-motion';

const recommendations = [
  {
    title: 'Meditation',
    icon: '🧘',
    duration: '10 min',
    focus: 'Reduce stress and improve presence',
    description: 'A short guided meditation can help settle the mind after a demanding day.',
  },
  {
    title: 'Breathing Exercises',
    icon: '🌬️',
    duration: '5 min',
    focus: 'Calm the nervous system',
    description: 'Try slow inhale-exhale cycles to regain steadiness and ease tension.',
  },
  {
    title: 'Journaling',
    icon: '📝',
    duration: '8 min',
    focus: 'Reflect on emotions and patterns',
    description: 'Capture thoughts in a simple entry to make feelings easier to understand.',
  },
  {
    title: 'Walking',
    icon: '🚶',
    duration: '20 min',
    focus: 'Support energy and mood',
    description: 'A gentle walk can bring clarity, movement, and fresh perspective.',
  },
  {
    title: 'Sleep Improvement',
    icon: '🌙',
    duration: '30 min before bed',
    focus: 'Create a calmer wind-down',
    description: 'Reduce screen time and keep the environment dim to support rest.',
  },
  {
    title: 'Music Therapy',
    icon: '🎵',
    duration: '15 min',
    focus: 'Improve emotional balance',
    description: 'Soft instrumental music can support a sense of calm and comfort.',
  },
  {
    title: 'Professional Consultation',
    icon: '🩺',
    duration: 'As needed',
    focus: 'Add expert support when useful',
    description: 'A professional conversation can be a strong next step if emotions feel overwhelming.',
  },
];

export function RecoveryPlanPage() {
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
            These recommendations are mock suggestions designed to feel practical, supportive, and easy to explore.
          </p>
        </div>
      </motion.section>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.05 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {recommendations.map((item) => (
            <motion.div
              key={item.title}
              whileHover={{ y: -4, scale: 1.01 }}
              className="rounded-[1.4rem] border border-slate-200 bg-slate-50 p-5"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-3xl">{item.icon}</p>
                  <h2 className="mt-3 text-lg font-semibold text-slate-900">{item.title}</h2>
                </div>
                <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600 shadow-sm">
                  {item.duration}
                </span>
              </div>
              <p className="mt-3 text-sm font-medium text-cyan-700">{item.focus}</p>
              <p className="mt-2 text-sm leading-7 text-slate-600">{item.description}</p>
            </motion.div>
          ))}
        </div>
      </motion.section>
    </div>
  );
}
