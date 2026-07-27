import { useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import { useAnalyzeSocial } from '../lib/analysis';
import { ApiError } from '../lib/api';

export function SocialMediaAnalysisPage() {
  const analyzeSocial = useAnalyzeSocial();
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  const analysis = analyzeSocial.data;

  const handleAnalyze = async () => {
    setError('');
    if (!text.trim()) {
      setError('Paste a social media post to analyze.');
      return;
    }
    try {
      await analyzeSocial.mutateAsync(text.trim());
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
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Social Media Input</p>
          <h1 className="mt-2 text-3xl font-semibold">Analyze a social post</h1>
          <p className="mt-3 text-sm leading-7 text-slate-300">
            Paste a recent social media post to detect its sentiment, emotion, and likely mental-health signals with a
            transparent, explainable breakdown. One of three assessment methods alongside the questionnaire and journal.
          </p>
        </div>
      </motion.section>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.05 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <label htmlFor="social-text" className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">
          Your post
        </label>
        <textarea
          id="social-text"
          value={text}
          onChange={(event) => setText(event.target.value)}
          rows={5}
          placeholder="Paste your post here…"
          className="mt-4 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
        />
        <div className="mt-3 flex items-center gap-3">
          <Button type="button" onClick={handleAnalyze} disabled={analyzeSocial.isPending}>
            {analyzeSocial.isPending ? 'Analyzing…' : 'Analyze'}
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
              <p className="text-xs text-slate-500">Confidence: {(analysis.predictionConfidence * 100).toFixed(0)}%</p>
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
          </div>
        ) : null}
      </motion.section>
    </div>
  );
}
