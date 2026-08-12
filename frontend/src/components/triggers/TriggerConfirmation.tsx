import { useState } from 'react';
import { useConfirmTrigger, useDismissTrigger, usePendingTriggers } from '../../lib/triggers';

/**
 * Confirm/dismiss chips for triggers the ML service detected in the user's writing.
 *
 * <p>Both answers are valuable: a confirmation says the detection was right, a dismissal
 * removes it from analytics and records that the lexicon over-matched.
 */
export function TriggerConfirmation({ compact = false }: { compact?: boolean }) {
  const { data: pending, isLoading } = usePendingTriggers();
  const confirm = useConfirmTrigger();
  const dismiss = useDismissTrigger();
  const [adjusting, setAdjusting] = useState<number | null>(null);
  // Held locally: the trigger objects come from the query cache and must not be mutated.
  const [adjustedIntensity, setAdjustedIntensity] = useState(5);

  if (isLoading || !pending || pending.length === 0) {
    return null;
  }

  return (
    <section className="rounded-[1.5rem] border border-amber-200 bg-amber-50/70 p-5 dark:border-amber-900/40 dark:bg-amber-950/20">
      <p className="text-sm font-semibold text-amber-900 dark:text-amber-100">
        We spotted {pending.length} possible trigger{pending.length === 1 ? '' : 's'} in your writing
      </p>
      <p className="mt-1 text-xs text-amber-800 dark:text-amber-300">
        Confirm the ones that felt real. Dismissing removes it from your analytics.
      </p>

      <div className="mt-4 space-y-2">
        {pending.slice(0, compact ? 3 : pending.length).map((trigger) => (
          <div
            key={trigger.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-white px-4 py-3 dark:bg-slate-900"
          >
            <div className="min-w-0">
              <p className="text-sm font-semibold text-slate-900 dark:text-white">
                {trigger.category}
                <span className="ml-2 text-xs font-normal text-slate-500">intensity {trigger.intensity}/10</span>
              </p>
              {trigger.note ? (
                <p className="truncate text-xs text-slate-500 dark:text-slate-400">{trigger.note}</p>
              ) : null}
            </div>

            <div className="flex items-center gap-2">
              {adjusting === trigger.id ? (
                <>
                  <label className="sr-only" htmlFor={`intensity-${trigger.id}`}>
                    Intensity for {trigger.category}
                  </label>
                  <input
                    id={`intensity-${trigger.id}`}
                    type="range"
                    min={1}
                    max={10}
                    value={adjustedIntensity}
                    onChange={(event) => setAdjustedIntensity(Number(event.target.value))}
                    className="w-28"
                  />
                  <span className="text-xs font-semibold text-slate-700 dark:text-slate-200">
                    {adjustedIntensity}
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      confirm.mutate(
                        { id: trigger.id, intensity: adjustedIntensity },
                        { onSuccess: () => setAdjusting(null) },
                      )
                    }
                    className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white"
                  >
                    Save
                  </button>
                </>
              ) : (
                <>
                  <button
                    type="button"
                    disabled={confirm.isPending}
                    onClick={() => confirm.mutate({ id: trigger.id })}
                    className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-500 disabled:opacity-60"
                  >
                    Confirm
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setAdjustedIntensity(trigger.intensity);
                      setAdjusting(trigger.id);
                    }}
                    className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200"
                  >
                    Adjust
                  </button>
                  <button
                    type="button"
                    disabled={dismiss.isPending}
                    onClick={() => dismiss.mutate(trigger.id)}
                    className="rounded-lg px-3 py-1.5 text-xs font-medium text-slate-500 transition hover:bg-slate-100 disabled:opacity-60 dark:hover:bg-slate-800"
                  >
                    Not this
                  </button>
                </>
              )}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
