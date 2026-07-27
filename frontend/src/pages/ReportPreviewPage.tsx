import { useRef, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line, Bar } from 'react-chartjs-2';
import { Button } from '../components/ui/button';
import { useReportSummary } from '../lib/reports';
import { useAnalyticsOverview } from '../lib/analytics';
import { exportElementToPdf } from '../lib/pdf';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Filler, Tooltip, Legend);

export function ReportPreviewPage() {
  const { data: report, isLoading } = useReportSummary();
  const { data: analytics } = useAnalyticsOverview();
  const reportRef = useRef<HTMLDivElement>(null);
  const [exporting, setExporting] = useState(false);

  const handleDownload = async () => {
    if (!reportRef.current) return;
    setExporting(true);
    try {
      await exportElementToPdf(reportRef.current, 'mindmirror-wellness-report.pdf');
    } finally {
      setExporting(false);
    }
  };

  const summaryCards = [
    { label: 'Overall Wellness', value: report ? `${report.overallWellness}/100` : '—' },
    { label: 'Stress Trend', value: report?.stressTrend ?? '—' },
    { label: 'Recovery Score', value: report ? `${report.recoveryScore}%` : '—' },
  ];
  const predictionSummary = report?.predictionSummary ?? [];
  const triggerAnalysis = (report?.topTriggers ?? []).map((t) => ({ title: t.title, strength: t.strength }));
  const recommendations = report?.recommendations ?? [];

  return (
    <div className="space-y-6" ref={reportRef}>
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">PDF Report Preview</p>
            <h1 className="mt-2 text-3xl font-semibold text-slate-900">Preview your wellness report</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">
              {isLoading ? 'Compiling your wellness report…' : 'A live snapshot of your wellbeing, aggregated from your activity.'}
            </p>
          </div>
          <Button
            data-html2canvas-ignore
            onClick={handleDownload}
            disabled={isLoading || exporting || !report}
            className="w-full sm:w-auto"
          >
            {exporting ? 'Generating…' : 'Download Report'}
          </Button>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {summaryCards.map((card) => (
            <div key={card.label} className="rounded-[1.4rem] border border-slate-200 bg-slate-50 p-5">
              <p className="text-sm text-slate-500">{card.label}</p>
              <p className="mt-2 text-xl font-semibold text-slate-900">{card.value}</p>
            </div>
          ))}
        </div>
      </motion.section>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.05 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Wellness Summary</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">What your current snapshot says</h2>
          <div className="mt-6 rounded-[1.4rem] border border-slate-200 bg-slate-50 p-5 text-sm leading-7 text-slate-600">
            {report?.summaryText ?? 'Your report will appear here once you start logging journals, moods, and check-ins.'}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Prediction Summary</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Short-term outlook</h2>
          <ul className="mt-6 space-y-3 text-sm leading-7 text-slate-600">
            {predictionSummary.map((item) => (
              <li key={item} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                {item}
              </li>
            ))}
          </ul>
        </motion.section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.1 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Trigger Analysis</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Most relevant pressure points</h2>
          <div className="mt-6 space-y-3">
            {triggerAnalysis.map((item) => (
              <div key={item.title} className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <span className="text-sm font-medium text-slate-800">{item.title}</span>
                <span className="text-sm font-semibold text-indigo-600">{item.strength}</span>
              </div>
            ))}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.12 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Charts</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Visual snapshot</h2>
          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            <div className="rounded-[1.4rem] border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-900">Mood trend</p>
              <div className="mt-3">
                {analytics && analytics.moodSeries.length > 0 ? (
                  <Line
                    data={{
                      labels: analytics.moodSeries.map((p) => p.date.slice(5)),
                      datasets: [
                        {
                          data: analytics.moodSeries.map((p) => p.score),
                          borderColor: '#6366f1',
                          backgroundColor: 'rgba(99, 102, 241, 0.15)',
                          tension: 0.35,
                          fill: true,
                        },
                      ],
                    }}
                    options={{ plugins: { legend: { display: false } }, scales: { y: { min: 0, max: 100 } } }}
                  />
                ) : (
                  <div className="h-20 rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-500" />
                )}
              </div>
            </div>
            <div className="rounded-[1.4rem] border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-900">Trigger intensity</p>
              <div className="mt-3">
                {analytics && analytics.triggerDistribution.length > 0 ? (
                  <Bar
                    data={{
                      labels: analytics.triggerDistribution.map((m) => m.label),
                      datasets: [
                        {
                          data: analytics.triggerDistribution.map((m) => m.value),
                          backgroundColor: '#f59e0b',
                          borderRadius: 8,
                        },
                      ],
                    }}
                    options={{ plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } }}
                  />
                ) : (
                  <div className="h-20 rounded-xl bg-gradient-to-r from-amber-400 to-rose-500" />
                )}
              </div>
            </div>
          </div>
        </motion.section>
      </div>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.14 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Recommendations</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900">Suggested next steps</h2>
        <div className="mt-6 space-y-3">
          {recommendations.map((item) => (
            <div key={item} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-7 text-slate-600">
              {item}
            </div>
          ))}
        </div>
      </motion.section>
    </div>
  );
}
