import { bandTone, confidenceLabel, deviationLabel, useMindset } from '../../lib/mindset';

const TONE_CLASSES: Record<string, string> = {
  positive: 'bg-emerald-100 text-emerald-700',
  neutral: 'bg-sky-100 text-sky-700',
  caution: 'bg-amber-100 text-amber-800',
  concern: 'bg-rose-100 text-rose-700',
  unknown: 'bg-slate-100 text-slate-600',
};

/**
 * The composite score, broken down.
 *
 * <p>Shows every component's contribution rather than a bare number, and frames the reading
 * as deviation from the user's own baseline instead of an absolute judgement.
 */
export function MindsetPanel({ compact = false }: { compact?: boolean }) {
  const { data: mindset, isLoading } = useMindset();

  if (isLoading) {
    return (
      <div className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <p className="text-sm text-slate-500">Loading your reading…</p>
      </div>
    );
  }

  if (!mindset) {
    return null;
  }

  const tone = TONE_CLASSES[bandTone(mindset.band)];
  const deviation = deviationLabel(mindset.deviationSigma);

  return (
    <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-300">
            Your reading
          </p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">
            {mindset.score === null ? 'Not enough data yet' : `${mindset.score}/100`}
          </h2>
          <p className="mt-2 max-w-xl text-sm leading-7 text-slate-600 dark:text-slate-300">
            {mindset.insight}
          </p>
        </div>
        {mindset.band ? (
          <span className={`rounded-full px-3 py-1 text-sm font-semibold ${tone}`}>{mindset.band}</span>
        ) : null}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
        <span>{confidenceLabel(mindset.confidence)}</span>
        {deviation ? <span>· {deviation}</span> : null}
        {!mindset.baselineEstablished ? (
          <span>
            · Building your baseline ({mindset.baselineDaysCovered} day
            {mindset.baselineDaysCovered === 1 ? '' : 's'} of history)
          </span>
        ) : null}
      </div>

      {mindset.components.length > 0 && !compact ? (
        <div className="mt-6">
          <p className="text-sm font-medium text-slate-700 dark:text-slate-200">What went into this</p>
          <div className="mt-3 space-y-3">
            {mindset.components.map((component) => (
              <div key={component.label}>
                <div className="flex items-center justify-between text-xs text-slate-600 dark:text-slate-300">
                  <span className="font-medium">
                    {component.label}
                    <span className="ml-2 font-normal text-slate-400">{component.detail}</span>
                  </span>
                  <span>
                    {component.value} · {Math.round(component.weight * 100)}% of score
                  </span>
                </div>
                <div className="mt-1 h-2 rounded-full bg-slate-200 dark:bg-slate-700">
                  <div
                    className="h-2 rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500"
                    style={{ width: `${component.value}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : null}

      {mindset.score === null ? (
        <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
          We need at least two kinds of signal — a reflection, a check-in, or a questionnaire — before
          putting a number on it.
        </p>
      ) : null}
    </section>
  );
}
