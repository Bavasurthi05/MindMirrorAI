import { useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import { useAnalyzeJournal, useMoodPrediction } from '../lib/analysis';
import { ApiError } from '../lib/api';

const trendColor: Record<string, string> = {
  improving: 'text-emerald-600',
  steady: 'text-indigo-600',
  declining: 'text-rose-600',
  unknown: 'text-slate-500',
};

export function PredictionResultsPage() {
  const { data: prediction, isLoading, isError } = useMoodPrediction();
  const analyzeJournal = useAnalyzeJournal();
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  const analysis = analyzeJournal.data;

  const handleAnalyze = async () => {
    setError('');
    if (!text.trim()) {
      setError('Enter some text to analyze.');
      return;
    }
    try {
      await analyzeJournal.mutateAsync(text.trim());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Analysis failed. The ML service may be unavailable.');
    }
  };

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
      >
        <div className="max-w-3xl">
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Prediction Results</p>
          <h1 className="mt-2 text-3xl font-semibold">AI-driven analysis outcomes</h1>
          <p className="mt-3 text-sm leading-7 text-slate-300">
            A short-term mood projection from your recent check-ins, plus on-demand journal sentiment analysis with
            transparent, explainable signals.
          </p>
        </div>
      </motion.section>

      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.05 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Mood Prediction</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Short-term outlook</h2>
          {isLoading ? (
            <p className="mt-6 text-sm text-slate-500">Loading prediction…</p>
          ) : isError || !prediction ? (
            <p className="mt-6 text-sm text-slate-500">Prediction unavailable. Log a few moods to enable this.</p>
          ) : (
            <div className="mt-6 space-y-4">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
                <p className="text-sm text-slate-500">Predicted mood score</p>
                <p className="mt-1 text-4xl font-semibold text-slate-900">{prediction.predictedScore}</p>
                <p className={`mt-2 text-sm font-semibold capitalize ${trendColor[prediction.trend] ?? 'text-slate-600'}`}>
                  {prediction.trend}
                </p>
              </div>
              <p className="text-sm leading-6 text-slate-600">{prediction.rationale}</p>
              <p className="text-xs text-slate-400">Confidence: {(prediction.confidence * 100).toFixed(0)}%</p>
            </div>
          )}
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Journal Analysis</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Analyze a reflection</h2>
          <textarea
            value={text}
            onChange={(event) => setText(event.target.value)}
            rows={5}
            placeholder="Paste or write a journal entry to analyze its sentiment and emotion…"
            className="mt-4 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
          />
          <div className="mt-3 flex items-center gap-3">
            <Button type="button" onClick={handleAnalyze} disabled={analyzeJournal.isPending}>
              {analyzeJournal.isPending ? 'Analyzing…' : 'Analyze'}
            </Button>
            {error ? <span className="text-sm text-rose-600">{error}</span> : null}
          </div>

          {analysis ? (
            <div className="mt-6 space-y-4">
              <div className="flex flex-wrap gap-3">
                <span className="rounded-full bg-indigo-600 px-3 py-1 text-sm font-semibold capitalize text-white">
                  {analysis.sentiment}
                </span>
                <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold capitalize text-slate-700">
                  {analysis.emotion}
                </span>
                <span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600">
                  score {analysis.sentimentScore}
                </span>
              </div>

              <div className="rounded-2xl border border-indigo-100 bg-indigo-50/60 p-5">
                <div className="flex items-baseline justify-between">
                  <p className="text-sm font-medium text-indigo-700">Predicted state</p>
                  <span className="text-[11px] font-medium uppercase tracking-wide text-indigo-400">
                    {analysis.modelBackend === 'random_forest' ? 'Random Forest' : 'Heuristic'}
                  </span>
                </div>
                <p className="mt-1 text-2xl font-semibold capitalize text-slate-900">{analysis.prediction}</p>
                <p className="text-xs text-slate-500">
                  Confidence: {(analysis.predictionConfidence * 100).toFixed(0)}%
                </p>
              </div>

              {analysis.reasons.length > 0 ? (
                <div>
                  <p className="text-sm font-medium text-slate-700">Why this prediction (Explainable AI)</p>
                  <div className="mt-3 space-y-2">
                    {analysis.reasons.map((reason) => (
                      <div key={reason.feature}>
                        <div className="flex items-center justify-between text-xs text-slate-600">
                          <span className="font-medium capitalize">{reason.feature}</span>
                          <span>{reason.percentage}%</span>
                        </div>
                        <div className="mt-1 h-2 w-full overflow-hidden rounded-full bg-slate-100">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500"
                            style={{ width: `${Math.min(100, reason.percentage)}%` }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              {analysis.explanation.length > 0 ? (
                <div>
                  <p className="text-sm font-medium text-slate-700">Key contributing words</p>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {analysis.explanation.map((item) => (
                      <span
                        key={item.token}
                        className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                          item.weight >= 0 ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
                        }`}
                      >
                        {item.token}
                      </span>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
          ) : null}
        </motion.section>
      </div>
    </div>
  );
}
