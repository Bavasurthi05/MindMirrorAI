import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  ArcElement,
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import { Doughnut, Line } from 'react-chartjs-2';
import { ApiError } from '../lib/api';
import {
  PROVIDER_GUIDES,
  importProgress,
  providerLabel,
  skippedSummary,
  useDeleteSocialImport,
  useSocialImports,
  useSocialInsights,
  useUploadSocialExport,
  validateExportFile,
  type SocialImport,
  type SocialProvider,
} from '../lib/social-imports';

ChartJS.register(ArcElement, CategoryScale, Filler, Legend, LineElement, LinearScale, PointElement, Tooltip);

const cardClass =
  'rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900';
const eyebrowClass = 'text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-300';

const PREDICTION_STYLE: Record<string, { label: string; color: string; chip: string }> = {
  normal: { label: 'Steady', color: '#10b981', chip: 'bg-emerald-100 text-emerald-700' },
  stress: { label: 'Stress', color: '#f59e0b', chip: 'bg-amber-100 text-amber-800' },
  anxiety: { label: 'Anxiety', color: '#6366f1', chip: 'bg-indigo-100 text-indigo-700' },
  depression: { label: 'Low mood', color: '#fb7185', chip: 'bg-rose-100 text-rose-700' },
};

function predictionStyle(prediction: string | null) {
  return (
    (prediction ? PREDICTION_STYLE[prediction] : undefined) ?? {
      label: prediction ?? 'Unclassified',
      color: '#94a3b8',
      chip: 'bg-slate-100 text-slate-700',
    }
  );
}

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

function ImportRow({
  item,
  onDelete,
  deleting,
}: {
  item: SocialImport;
  onDelete: (item: SocialImport) => void;
  deleting: boolean;
}) {
  const processing = item.status === 'PROCESSING';
  const progress = importProgress(item);
  const skipped = skippedSummary(item);

  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/60">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-semibold text-slate-900 dark:text-white">
            {providerLabel(item.provider)}
            {item.originalFilename ? (
              <span className="ml-2 break-all text-sm font-normal text-slate-500">{item.originalFilename}</span>
            ) : null}
          </p>
          <p className="text-sm text-slate-600 dark:text-slate-300">
            Uploaded {formatDate(item.createdAt)}
            {item.earliestPostAt
              ? ` · posts from ${formatDate(item.earliestPostAt)} to ${formatDate(item.latestPostAt)}`
              : ''}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span
            className={`rounded-full px-3 py-1 text-xs font-semibold ${
              processing ? 'bg-cyan-100 text-cyan-800' : 'bg-emerald-100 text-emerald-700'
            }`}
          >
            {processing ? 'Analyzing' : 'Done'}
          </span>
          <button
            type="button"
            onClick={() => onDelete(item)}
            disabled={deleting}
            className="rounded-lg border border-slate-300 px-3 py-1 text-xs font-medium text-slate-700 transition hover:bg-white disabled:opacity-60 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-700"
          >
            Delete
          </button>
        </div>
      </div>

      {processing ? (
        <div className="mt-3">
          <div className="h-2 rounded-full bg-slate-200 dark:bg-slate-700">
            <div
              className="h-2 rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500 transition-all"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {item.postsAnalyzed + item.postsFailed} of {item.postsImported} posts analyzed
          </p>
        </div>
      ) : null}

      <p className="mt-3 text-sm text-slate-700 dark:text-slate-200">
        {item.postsAnalyzed} {item.postsAnalyzed === 1 ? 'post' : 'posts'} analyzed
        {item.postsFailed > 0 ? ` · ${item.postsFailed} couldn't be analyzed yet and will be retried` : ''}
      </p>
      {skipped.length > 0 ? (
        <ul className="mt-1 list-inside list-disc text-xs text-slate-500 dark:text-slate-400">
          {skipped.map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

export function SocialImportPage() {
  const [provider, setProvider] = useState<SocialProvider>('X');
  const [file, setFile] = useState<File | null>(null);
  const [acknowledged, setAcknowledged] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  // Bumped to clear the file input after a successful upload.
  const [fileInputKey, setFileInputKey] = useState(0);

  const imports = useSocialImports();
  const importList = imports.data ?? [];
  const processing = importList.some((item) => item.status === 'PROCESSING');
  const insights = useSocialInsights(processing);
  const upload = useUploadSocialExport();
  const remove = useDeleteSocialImport();

  const guide = PROVIDER_GUIDES.find((item) => item.id === provider) ?? PROVIDER_GUIDES[0];

  const handleUpload = async () => {
    setError(null);
    setNotice(null);
    const problem = validateExportFile(file);
    if (problem) {
      setError(problem);
      return;
    }
    if (!acknowledged || !file) {
      setError('Please confirm you understand how your posts will be used.');
      return;
    }
    try {
      const result = await upload.mutateAsync({ provider, file, acknowledged });
      setNotice(
        result.postsImported === 0
          ? 'No new posts to analyze — everything in that file was already imported, or had no text.'
          : `Found ${result.postsImported} ${result.postsImported === 1 ? 'post' : 'posts'}. Analyzing them now — you can leave this page.`,
      );
      setFile(null);
      setFileInputKey((key) => key + 1);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'The upload failed. Please try again.');
    }
  };

  const handleDelete = async (item: SocialImport) => {
    const confirmed = window.confirm(
      `Delete this ${providerLabel(item.provider)} import? Its posts and their analysis will be permanently removed.`,
    );
    if (!confirmed) return;
    setError(null);
    try {
      await remove.mutateAsync(item.id);
      setNotice('Import deleted, along with its posts and analysis.');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete that import.');
    }
  };

  const data = insights.data;
  const timeline = data?.timeline ?? [];
  const timelineData = {
    labels: timeline.map((point) => point.label),
    datasets: [
      {
        label: 'Average tone',
        data: timeline.map((point) => point.averageSentiment),
        borderColor: '#22d3ee',
        backgroundColor: 'rgba(34, 211, 238, 0.15)',
        tension: 0.35,
        fill: true,
        yAxisID: 'tone',
      },
      {
        label: 'Stress, anxiety or low mood (%)',
        data: timeline.map((point) => Math.round(point.concernShare * 100)),
        borderColor: '#fb7185',
        backgroundColor: 'rgba(251, 113, 133, 0.1)',
        tension: 0.35,
        yAxisID: 'share',
      },
    ],
  };

  const distribution = Object.entries(data?.predictionDistribution ?? {});
  const distributionData = {
    labels: distribution.map(([prediction]) => predictionStyle(prediction).label),
    datasets: [
      {
        data: distribution.map(([, count]) => count),
        backgroundColor: distribution.map(([prediction]) => predictionStyle(prediction).color),
        borderWidth: 0,
      },
    ],
  };
  const maxEmotionCount = Math.max(1, ...(data?.topEmotions ?? []).map((item) => item.count));

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Social media</p>
        <h1 className="mt-2 text-3xl font-semibold">Import your posts</h1>
        <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-300">
          Download your data from X, Instagram or Facebook, upload it here, and see what the language of your
          posts suggests over time.
        </p>
        <ul className="mt-4 grid gap-2 text-sm text-slate-200 sm:grid-cols-2">
          <li>• Your file is read once and then discarded.</li>
          <li>• Only the text and date of your own posts are kept.</li>
          <li>• Messages and other people&rsquo;s posts are never read.</li>
          <li>• Delete an import at any time to remove its posts and analysis.</li>
        </ul>
      </motion.section>

      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <section className={cardClass}>
          <p className={eyebrowClass}>Step 1</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">Get your export</h2>

          <div className="mt-5 flex flex-wrap gap-2" role="tablist" aria-label="Platform">
            {PROVIDER_GUIDES.map((item) => (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={provider === item.id}
                onClick={() => setProvider(item.id)}
                className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${
                  provider === item.id
                    ? 'border-indigo-500 bg-indigo-600 text-white'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-indigo-300 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200'
                }`}
              >
                {item.name}
              </button>
            ))}
          </div>

          <ol className="mt-5 list-inside list-decimal space-y-2 text-sm leading-6 text-slate-700 dark:text-slate-200">
            {guide.steps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
          <div className="mt-5 rounded-2xl bg-slate-50 p-4 text-sm dark:bg-slate-800/60">
            <p className="font-medium text-slate-900 dark:text-white">What to upload</p>
            <p className="mt-1 text-slate-600 dark:text-slate-300">{guide.upload}</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">{guide.note}</p>
          </div>
        </section>

        <section className={cardClass}>
          <p className={eyebrowClass}>Step 2</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">
            Upload your {providerLabel(provider)} export
          </h2>

          <label className="mt-5 block text-sm font-medium text-slate-700 dark:text-slate-200" htmlFor="export-file">
            Export file (.zip, .json or .js, up to 100 MB)
          </label>
          <input
            key={fileInputKey}
            id="export-file"
            type="file"
            accept=".zip,.json,.js"
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null);
              setError(null);
            }}
            className="mt-2 block w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 file:mr-4 file:rounded-xl file:border-0 file:bg-indigo-600 file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-white dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
          />

          <label className="mt-5 flex items-start gap-3 rounded-2xl border border-slate-200 p-4 text-sm dark:border-slate-700">
            <input
              type="checkbox"
              checked={acknowledged}
              onChange={(event) => setAcknowledged(event.target.checked)}
              className="mt-1 h-4 w-4"
            />
            <span className="text-slate-700 dark:text-slate-200">
              I understand my posts will be analyzed for wellbeing signals. The results describe patterns in my
              writing and are not a diagnosis.
              <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">
                Imported posts are kept separate from your daily dashboard and wellness score, and are not used to
                train the model.
              </span>
            </span>
          </label>

          <button
            type="button"
            onClick={handleUpload}
            disabled={upload.isPending || !file || !acknowledged}
            className="mt-5 rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {upload.isPending ? 'Uploading…' : 'Upload and analyze'}
          </button>

          {error ? <p className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p> : null}
          {notice ? (
            <p className="mt-4 rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{notice}</p>
          ) : null}
        </section>
      </div>

      <section className={cardClass}>
        <p className={eyebrowClass}>Your imports</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">Upload history</h2>
        <div className="mt-6 space-y-3">
          {imports.isLoading ? (
            <p className="text-sm text-slate-500">Loading…</p>
          ) : importList.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">No imports yet.</p>
          ) : (
            importList.map((item) => (
              <ImportRow key={item.id} item={item} onDelete={handleDelete} deleting={remove.isPending} />
            ))
          )}
        </div>
      </section>

      <section className={cardClass}>
        <p className={eyebrowClass}>Insights</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">What your posts suggest</h2>
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
          Patterns in the language of your posts, grouped by when you posted them. These readings are not a
          diagnosis.
        </p>

        {!data || data.analyzedPosts === 0 ? (
          <p className="mt-6 text-sm text-slate-500 dark:text-slate-400">
            {data && data.pendingPosts > 0
              ? `Analyzing ${data.pendingPosts} ${data.pendingPosts === 1 ? 'post' : 'posts'} — insights appear as they finish.`
              : 'Upload an export to see insights here.'}
          </p>
        ) : (
          <>
            <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {[
                { label: 'Posts analyzed', value: data.analyzedPosts.toLocaleString() },
                {
                  label: 'Average tone',
                  value: data.averageSentiment === null ? '—' : data.averageSentiment.toFixed(2),
                  detail: 'From −1 (negative) to 1 (positive)',
                },
                { label: 'Months covered', value: String(data.timeline.length) },
                {
                  label: 'Still analyzing',
                  value: String(data.pendingPosts),
                },
              ].map((tile) => (
                <div key={tile.label} className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-800/60">
                  <p className="text-sm text-slate-500 dark:text-slate-400">{tile.label}</p>
                  <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">{tile.value}</p>
                  {tile.detail ? <p className="mt-1 text-xs text-slate-500">{tile.detail}</p> : null}
                </div>
              ))}
            </div>

            <div className="mt-6 grid gap-6 xl:grid-cols-[1.4fr_0.6fr]">
              <div>
                <p className="text-sm font-medium text-slate-700 dark:text-slate-200">By month</p>
                {timeline.length === 0 ? (
                  <p className="mt-2 text-sm text-slate-500">None of the analyzed posts carried a date.</p>
                ) : (
                  <div className="mt-2">
                    <Line
                      data={timelineData}
                      options={{
                        interaction: { mode: 'index', intersect: false },
                        scales: {
                          tone: {
                            type: 'linear',
                            position: 'left',
                            min: -1,
                            max: 1,
                            title: { display: true, text: 'Tone' },
                          },
                          share: {
                            type: 'linear',
                            position: 'right',
                            min: 0,
                            max: 100,
                            grid: { drawOnChartArea: false },
                            title: { display: true, text: '% of posts' },
                          },
                        },
                      }}
                    />
                  </div>
                )}
                {data.undatedPosts > 0 ? (
                  <p className="mt-2 text-xs text-slate-500">
                    {data.undatedPosts} analyzed {data.undatedPosts === 1 ? 'post has' : 'posts have'} no date and
                    {data.undatedPosts === 1 ? ' is' : ' are'} not shown on the timeline.
                  </p>
                ) : null}
              </div>

              <div className="space-y-6">
                <div>
                  <p className="text-sm font-medium text-slate-700 dark:text-slate-200">Overall reading</p>
                  <div className="mx-auto mt-2 max-w-[220px]">
                    <Doughnut data={distributionData} options={{ plugins: { legend: { position: 'bottom' } } }} />
                  </div>
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-700 dark:text-slate-200">Most common emotions</p>
                  <div className="mt-2 space-y-2">
                    {data.topEmotions.map((item) => (
                      <div key={item.emotion}>
                        <div className="flex justify-between text-xs capitalize text-slate-600 dark:text-slate-300">
                          <span>{item.emotion}</span>
                          <span>{item.count}</span>
                        </div>
                        <div className="mt-1 h-2 rounded-full bg-slate-200 dark:bg-slate-700">
                          <div
                            className="h-2 rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500"
                            style={{ width: `${(100 * item.count) / maxEmotionCount}%` }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-8">
              <p className="text-sm font-medium text-slate-700 dark:text-slate-200">Most recent posts</p>
              <div className="mt-3 space-y-3">
                {data.recentPosts.map((post) => {
                  const style = predictionStyle(post.prediction);
                  return (
                    <div
                      key={post.id}
                      className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/60"
                    >
                      <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                        <span className="font-semibold text-slate-700 dark:text-slate-200">
                          {providerLabel(post.provider)}
                        </span>
                        <span>{formatDate(post.postedAt)}</span>
                        <span className={`rounded-full px-2 py-0.5 font-semibold ${style.chip}`}>{style.label}</span>
                        {post.dominantEmotion ? <span className="capitalize">{post.dominantEmotion}</span> : null}
                      </div>
                      <p className="mt-2 whitespace-pre-line break-words text-sm text-slate-700 dark:text-slate-200">
                        {post.excerpt}
                      </p>
                    </div>
                  );
                })}
              </div>
            </div>
          </>
        )}
      </section>
    </div>
  );
}
