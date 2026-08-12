import { useState } from 'react';
import {
  useModelVersions,
  usePromoteModel,
  useRollbackModel,
  useStartRetrain,
  useTrainingDataSummary,
  useTrainingRunStatus,
  useTrainingRuns,
} from '../../lib/admin';
import { ApiError } from '../../lib/api';

const cardClass =
  'rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900';

/**
 * Retrain, compare, promote, roll back.
 *
 * <p>Producing a version is never the same as deploying one: a completed run is a
 * candidate until a person promotes it, and a version that failed the quality gate needs
 * an explicit, acknowledged override.
 */
export function ModelManagement() {
  const { data: summary } = useTrainingDataSummary();
  const { data: versions } = useModelVersions();
  const { data: runs } = useTrainingRuns(10);
  const startRetrain = useStartRetrain();
  const promote = usePromoteModel();
  const rollback = useRollbackModel();

  const [jobId, setJobId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { data: activeRun } = useTrainingRunStatus(jobId);

  const handleRetrain = async () => {
    setError(null);
    try {
      const run = await startRetrain.mutateAsync();
      setJobId(run.jobId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not start a training run.');
    }
  };

  const handlePromote = async (version: string, gatePassed: boolean) => {
    setError(null);
    if (!gatePassed) {
      const confirmed = window.confirm(
        `Version ${version} failed the quality gate. Deploying it will change the predictions ` +
          `every user sees. Continue anyway?`,
      );
      if (!confirmed) return;
    }
    try {
      await promote.mutateAsync({ version, force: !gatePassed });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not promote that version.');
    }
  };

  const handleRollback = async (version: string) => {
    setError(null);
    try {
      await rollback.mutateAsync(version);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not roll back.');
    }
  };

  const run = activeRun ?? runs?.[0];

  return (
    <section className={cardClass}>
      <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">
        Model management
      </p>
      <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">
        Retraining &amp; deployment
      </h2>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
        Retraining learns from consented user corrections on top of the synthetic seed corpus.
        A new version is never deployed automatically.
      </p>

      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-800">
          <p className="text-sm text-slate-500 dark:text-slate-400">Consenting users</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">
            {summary?.consentingUsers ?? '—'}
          </p>
        </div>
        <div className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-800">
          <p className="text-sm text-slate-500 dark:text-slate-400">Exportable examples</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">
            {summary?.exportableExamples ?? '—'}
          </p>
        </div>
        <div className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-800">
          <p className="text-sm text-slate-500 dark:text-slate-400">Live model</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">
            {versions?.active ?? 'None'}
          </p>
        </div>
      </div>

      {summary && Object.keys(summary.labelCounts).length > 0 ? (
        <div className="mt-3 flex flex-wrap gap-2">
          {Object.entries(summary.labelCounts).map(([label, count]) => (
            <span
              key={label}
              className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium capitalize text-slate-700 dark:bg-slate-800 dark:text-slate-200"
            >
              {label}: {count}
            </span>
          ))}
        </div>
      ) : null}

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={handleRetrain}
          disabled={startRetrain.isPending}
          className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:opacity-60"
        >
          {startRetrain.isPending ? 'Starting…' : 'Start training run'}
        </button>
        {run && (run.status === 'RUNNING' || run.status === 'PENDING') ? (
          <span className="text-sm text-slate-600 dark:text-slate-300">
            <span className="animate-pulse">Training…</span> {run.message}
          </span>
        ) : null}
      </div>

      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}

      {run && run.status === 'COMPLETED' ? (
        <div className="mt-6 rounded-2xl border border-slate-200 p-5 dark:border-slate-700">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-slate-900 dark:text-white">
                {run.version} — accuracy {run.accuracy ?? '—'} · F1 {run.f1Macro ?? '—'}
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Trained on {run.corpusTotal} examples ({run.corpusUserExamples} from users)
              </p>
            </div>
            <span
              className={`rounded-full px-3 py-1 text-xs font-semibold ${
                run.gatePassed ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-800'
              }`}
            >
              {run.gatePassed ? 'Gate passed' : 'Gate failed'}
            </span>
          </div>

          {run.gateChecks.length > 0 ? (
            <ul className="mt-4 space-y-1">
              {run.gateChecks.map((check) => (
                <li key={check.name} className="text-xs text-slate-600 dark:text-slate-300">
                  <span className={check.passed ? 'text-emerald-600' : 'text-rose-600'}>
                    {check.passed ? '✓' : '✗'}
                  </span>{' '}
                  <span className="font-medium">{check.name}</span> — {check.detail}
                </li>
              ))}
            </ul>
          ) : null}

          {run.version ? (
            <button
              type="button"
              onClick={() => handlePromote(run.version as string, run.gatePassed)}
              disabled={promote.isPending}
              className="mt-4 rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-700 disabled:opacity-60"
            >
              {run.gatePassed ? `Promote ${run.version}` : `Force-promote ${run.version}`}
            </button>
          ) : null}
        </div>
      ) : null}

      {versions && versions.versions.length > 0 ? (
        <div className="mt-8">
          <p className="text-sm font-semibold text-slate-900 dark:text-white">Versions</p>
          <div className="mt-3 overflow-x-auto">
            <table className="w-full min-w-[560px] text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="py-2">Version</th>
                  <th className="py-2">Accuracy</th>
                  <th className="py-2">Gate</th>
                  <th className="py-2">Status</th>
                  <th className="py-2" />
                </tr>
              </thead>
              <tbody>
                {versions.versions.map((item) => {
                  const forest = (item.metrics?.models as Record<string, { accuracy?: number }>)
                    ?.random_forest;
                  const isActive = item.version === versions.active;
                  return (
                    <tr key={item.version} className="border-t border-slate-100 dark:border-slate-800">
                      <td className="py-2 font-medium text-slate-900 dark:text-white">{item.version}</td>
                      <td className="py-2 text-slate-600 dark:text-slate-300">{forest?.accuracy ?? '—'}</td>
                      <td className="py-2">
                        <span className={item.gate?.passed ? 'text-emerald-600' : 'text-amber-600'}>
                          {item.gate?.passed ? 'passed' : 'failed'}
                        </span>
                      </td>
                      <td className="py-2 text-slate-600 dark:text-slate-300">
                        {isActive ? <span className="font-semibold text-emerald-600">Live</span> : '—'}
                      </td>
                      <td className="py-2 text-right">
                        {isActive ? null : (
                          <button
                            type="button"
                            onClick={() => handleRollback(item.version)}
                            disabled={rollback.isPending}
                            className="rounded-lg border border-slate-300 px-3 py-1 text-xs font-medium text-slate-700 transition hover:bg-slate-50 disabled:opacity-60 dark:border-slate-600 dark:text-slate-200"
                          >
                            Roll back to this
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <p className="mt-6 text-sm text-slate-500 dark:text-slate-400">
          No trained versions yet. Run <code>python -m app.train</code> in the ML service to create a
          baseline, or start a training run once user corrections have accumulated.
        </p>
      )}

      {runs && runs.length > 0 ? (
        <div className="mt-8">
          <p className="text-sm font-semibold text-slate-900 dark:text-white">Recent runs</p>
          <ul className="mt-3 space-y-2">
            {runs.map((item) => (
              <li
                key={item.id}
                className="rounded-2xl bg-slate-50 px-4 py-2 text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300"
              >
                <span className="font-medium text-slate-900 dark:text-white">
                  {item.version ?? item.status}
                </span>{' '}
                · {item.status} · by {item.triggeredBy}
                {item.promoted ? ` · promoted${item.forced ? ' (forced)' : ''}` : ''}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}
