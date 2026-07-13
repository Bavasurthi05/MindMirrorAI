import { motion, AnimatePresence } from 'framer-motion';
import { type ReactNode } from 'react';
import { cn } from '../../lib/cn';

interface ToastProps {
  open: boolean;
  title?: string;
  message?: string;
  tone?: 'info' | 'success' | 'warning' | 'error';
  children?: ReactNode;
}

const toneClasses = {
  info: 'border-sky-200 bg-sky-50 text-sky-800',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  warning: 'border-amber-200 bg-amber-50 text-amber-800',
  error: 'border-rose-200 bg-rose-50 text-rose-800',
};

export function Toast({ open, title, message, tone = 'info', children }: ToastProps) {
  return (
    <AnimatePresence>
      {open ? (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: 8 }}
          className={cn('fixed bottom-6 right-6 max-w-sm rounded-2xl border p-4 shadow-lg', toneClasses[tone])}
        >
          {title ? <div className="font-semibold">{title}</div> : null}
          {message ? <div className="mt-1 text-sm">{message}</div> : null}
          {children}
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
