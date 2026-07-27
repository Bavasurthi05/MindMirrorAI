import { useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import { useSubmitFeedback } from '../lib/feedback';
import { ApiError } from '../lib/api';

export function FeedbackPage() {
  const submitFeedback = useSubmitFeedback();
  const [rating, setRating] = useState(0);
  const [message, setMessage] = useState('');
  const [status, setStatus] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleSubmit = async () => {
    setStatus(null);
    if (rating < 1) {
      setStatus({ type: 'error', text: 'Please choose a rating.' });
      return;
    }
    try {
      await submitFeedback.mutateAsync({ rating, message: message.trim() });
      setStatus({ type: 'success', text: 'Thank you! Your feedback has been submitted.' });
      setMessage('');
      setRating(0);
    } catch (err) {
      setStatus({ type: 'error', text: err instanceof ApiError ? err.message : 'Could not submit feedback.' });
    }
  };

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">Feedback &amp; Rating</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-900 dark:text-white">Help us improve</h1>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600 dark:text-slate-400">
          Share your experience and suggestions. Your feedback helps shape MindMirror AI.
        </p>

        <div className="mt-6">
          <p className="text-sm font-medium text-slate-700 dark:text-slate-300">Your rating</p>
          <div className="mt-2 flex gap-2">
            {[1, 2, 3, 4, 5].map((value) => (
              <button
                key={value}
                type="button"
                aria-label={`${value} star${value > 1 ? 's' : ''}`}
                onClick={() => setRating(value)}
                className={`text-3xl transition ${value <= rating ? 'text-amber-400' : 'text-slate-300 dark:text-slate-600'}`}
              >
                ★
              </button>
            ))}
          </div>
        </div>

        <textarea
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          rows={5}
          placeholder="What's working well? What could be better?"
          className="mt-4 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
        />

        <div className="mt-3 flex items-center gap-3">
          <Button type="button" onClick={handleSubmit} disabled={submitFeedback.isPending}>
            {submitFeedback.isPending ? 'Submitting…' : 'Submit feedback'}
          </Button>
          {status ? (
            <span className={`text-sm ${status.type === 'success' ? 'text-emerald-600' : 'text-rose-600'}`}>
              {status.text}
            </span>
          ) : null}
        </div>
      </motion.section>
    </div>
  );
}
