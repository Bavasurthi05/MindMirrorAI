import { useState } from 'react';
import { motion } from 'framer-motion';
import { MOOD_FACES, faceForScore, useSubmitCheckIn } from '../../lib/checkin';
import { ApiError } from '../../lib/api';
import type { DashboardCheckIn } from '../../lib/dashboard';

interface CheckInCardProps {
  checkIn: DashboardCheckIn;
}

/**
 * One-tap daily mood check-in.
 *
 * <p>The lowest-friction way to collect the data every chart depends on. Dismissible and never
 * blocking — a user who ignores it still gets a working (if emptier) app.
 */
export function CheckInCard({ checkIn }: CheckInCardProps) {
  const [dismissed, setDismissed] = useState(false);
  const [selected, setSelected] = useState<number | null>(checkIn.todaysScore);
  const [note, setNote] = useState('');
  const [showNote, setShowNote] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submit = useSubmitCheckIn();

  if (dismissed) {
    return null;
  }

  const alreadyDone = checkIn.checkedInToday && !submit.isSuccess;
  const savedFace = faceForScore(checkIn.todaysScore);

  const handleSelect = async (score: number, label: string) => {
    setSelected(score);
    setError(null);
    // A face alone is a complete check-in; the note is optional and saved on submit.
    if (showNote) {
      return;
    }
    try {
      await submit.mutateAsync({ moodScore: score, moodLabel: label });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your check-in.');
    }
  };

  const handleSubmitNote = async () => {
    if (selected === null) {
      setError('Pick how you are feeling first.');
      return;
    }
    setError(null);
    const face = MOOD_FACES.find((item) => item.score === selected);
    try {
      await submit.mutateAsync({ moodScore: selected, moodLabel: face?.label, note: note.trim() || undefined });
      setShowNote(false);
      setNote('');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your check-in.');
    }
  };

  return (
    <motion.section
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-[1.5rem] border border-indigo-200 bg-indigo-50/70 p-5 dark:border-indigo-900/50 dark:bg-indigo-950/30"
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-indigo-900 dark:text-indigo-100">
            {alreadyDone ? 'Checked in today' : 'How are you feeling today?'}
          </p>
          <p className="mt-1 text-xs text-indigo-700 dark:text-indigo-300">
            {alreadyDone && savedFace
              ? `You logged "${savedFace.label}". Tap another face to change it.`
              : 'One tap. It takes a second and powers everything on this page.'}
            {checkIn.streak > 1 ? ` · ${checkIn.streak}-day streak` : ''}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setDismissed(true)}
          className="rounded-lg px-2 py-1 text-xs text-indigo-600 transition hover:bg-indigo-100 dark:text-indigo-300"
        >
          Not now
        </button>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {MOOD_FACES.map((face) => (
          <button
            key={face.score}
            type="button"
            disabled={submit.isPending}
            onClick={() => handleSelect(face.score, face.label)}
            aria-pressed={selected === face.score}
            className={`flex min-w-[76px] flex-col items-center rounded-2xl border px-3 py-2 transition disabled:opacity-60 ${
              selected === face.score
                ? 'border-indigo-500 bg-white shadow-sm dark:bg-slate-900'
                : 'border-transparent bg-white/70 hover:border-indigo-300 dark:bg-slate-900/60'
            }`}
          >
            <span className="text-2xl">{face.emoji}</span>
            <span className="mt-1 text-xs font-medium text-slate-700 dark:text-slate-200">{face.label}</span>
          </button>
        ))}
      </div>

      {showNote ? (
        <div className="mt-3">
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            rows={2}
            maxLength={2000}
            placeholder="Anything you want to note? (analyzed like a journal entry)"
            className="w-full rounded-2xl border border-indigo-200 bg-white px-3 py-2 text-sm outline-none transition focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-900"
          />
          <div className="mt-2 flex gap-2">
            <button
              type="button"
              onClick={handleSubmitNote}
              disabled={submit.isPending}
              className="rounded-xl bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:opacity-60"
            >
              {submit.isPending ? 'Saving…' : 'Save check-in'}
            </button>
            <button
              type="button"
              onClick={() => setShowNote(false)}
              className="rounded-xl px-3 py-1.5 text-sm text-indigo-700 dark:text-indigo-300"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setShowNote(true)}
          className="mt-3 text-xs font-semibold text-indigo-700 underline-offset-2 hover:underline dark:text-indigo-300"
        >
          + Add a note
        </button>
      )}

      {error ? <p className="mt-2 text-xs text-rose-600">{error}</p> : null}
      {submit.isSuccess && !showNote ? (
        <p className="mt-2 text-xs font-medium text-emerald-700 dark:text-emerald-300">Saved — thank you.</p>
      ) : null}
    </motion.section>
  );
}
