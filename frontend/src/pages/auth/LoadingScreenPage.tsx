import { motion } from 'framer-motion';
import { Spinner } from '../../components/feedback/Spinner';

export function LoadingScreenPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 px-4">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="w-full max-w-md rounded-[2rem] border border-white/10 bg-white/10 p-8 text-center text-white shadow-2xl backdrop-blur"
      >
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-white/10">
          <Spinner className="h-7 w-7 border-slate-400 border-t-white" />
        </div>
        <h1 className="mt-6 text-2xl font-semibold">Preparing your workspace</h1>
        <p className="mt-3 text-sm leading-6 text-slate-300">
          This loading state is ready for future auth transitions, splash screens, or progress-based flows.
        </p>
      </motion.div>
    </div>
  );
}
