import { motion } from 'framer-motion';

const helplines = [
  { region: 'International', name: 'Befrienders Worldwide', contact: 'befrienders.org', note: 'Directory of local helplines worldwide.' },
  { region: 'United States', name: '988 Suicide & Crisis Lifeline', contact: 'Call or text 988', note: '24/7 free and confidential support.' },
  { region: 'United Kingdom', name: 'Samaritans', contact: 'Call 116 123', note: 'Available 24 hours a day, every day.' },
  { region: 'India', name: 'iCall / Vandrevala Foundation', contact: '9152987821 / 1860 266 2345', note: 'Free counselling support.' },
  { region: 'Canada', name: 'Talk Suicide Canada', contact: '1-833-456-4566', note: '24/7 support across Canada.' },
];

export function EmergencyHelpPage() {
  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-rose-200 bg-rose-50 p-8 shadow-sm dark:border-rose-900/50 dark:bg-rose-950/40"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-rose-600 dark:text-rose-300">Emergency Help</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-900 dark:text-white">You are not alone</h1>
        <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-700 dark:text-slate-300">
          If you are in immediate danger or thinking about harming yourself, please contact your local emergency
          services right away. The resources below are provided for informational purposes only and are not a
          substitute for professional care or crisis diagnosis.
        </p>
      </motion.section>

      <section className="grid gap-4 md:grid-cols-2">
        {helplines.map((line) => (
          <div
            key={line.name}
            className="rounded-[1.5rem] border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
          >
            <p className="text-xs font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">{line.region}</p>
            <h2 className="mt-2 text-lg font-semibold text-slate-900 dark:text-white">{line.name}</h2>
            <p className="mt-1 text-sm font-medium text-indigo-600 dark:text-indigo-300">{line.contact}</p>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{line.note}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
