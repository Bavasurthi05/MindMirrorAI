import { useState } from 'react';
import {
  PREDICTION_LABELS,
  usePredictionFeedback,
  useSubmitPredictionFeedback,
} from '../../lib/analysis';
import { ApiError } from '../../lib/api';

interface PredictionFeedbackControlProps {
  analysisId: number;
  predictedLabel: string | null;
}

/**
 * "Was this right?" — captures the single highest-quality training label the app can gather.
 *
 * <p>Agreeing confirms the predicted label; disagreeing requires the user to say which state
 * actually fits, which is what makes the answer trainable rather than just a complaint.
 */
export function PredictionFeedbackControl({ analysisId, predictedLabel }: PredictionFeedbackControlProps) {
  const { data: existing } = usePredictionFeedback(analysisId);
  const submit = useSubmitPredictionFeedback(analysisId);
  const [correcting, setCorrecting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const answered = existing ?? (submit.data || null);

  const send = async (agreement: 'AGREE' | 'DISAGREE', correctedLabel?: string) => {
    setError(null);
    try {
      await submit.mutateAsync({ agreement, correctedLabel });
      setCorrecting(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your feedback.');
    }
  };

  if (answered && !correcting) {
    return (
      <div className="mt-4 flex flex-wrap items-center gap-2 rounded-2xl bg-slate-50 px-4 py-3 text-sm dark:bg-slate-800/60">
        <span className="text-slate-700 dark:text-slate-200">
          {answered.agreement === 'AGREE'
            ? 'Thanks — noted that this was right.'
            : `Thanks — noted that this was closer to ${answered.correctedLabel}.`}
        </span>
        <button
          type="button"
          onClick={() => setCorrecting(true)}
          className="text-xs font-semibold text-indigo-600 underline-offset-2 hover:underline dark:text-indigo-300"
        >
          Change
        </button>
      </div>
    );
  }

  return (
    <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/60">
      {!correcting ? (
        <div className="flex flex-wrap items-center gap-3">
          <span className="text-sm font-medium text-slate-700 dark:text-slate-200">
            Does &ldquo;{predictedLabel ?? 'this'}&rdquo; feel right?
          </span>
          <button
            type="button"
            disabled={submit.isPending}
            onClick={() => send('AGREE')}
            className="rounded-xl bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white transition hover:bg-emerald-500 disabled:opacity-60"
          >
            Yes
          </button>
          <button
            type="button"
            disabled={submit.isPending}
            onClick={() => setCorrecting(true)}
            className="rounded-xl border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-white disabled:opacity-60 dark:border-slate-600 dark:text-slate-200"
          >
            Not quite
          </button>
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Your answer helps improve future insights.
          </span>
        </div>
      ) : (
        <div>
          <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
            Which fits better?
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            {PREDICTION_LABELS.map((label) => (
              <button
                key={label}
                type="button"
                disabled={submit.isPending}
                onClick={() => send('DISAGREE', label)}
                className="rounded-xl border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium capitalize text-slate-700 transition hover:border-indigo-400 hover:bg-indigo-50 disabled:opacity-60 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-200"
              >
                {label}
              </button>
            ))}
            <button
              type="button"
              onClick={() => setCorrecting(false)}
              className="rounded-xl px-3 py-1.5 text-sm text-slate-500"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
      {error ? <p className="mt-2 text-xs text-rose-600">{error}</p> : null}
    </div>
  );
}
