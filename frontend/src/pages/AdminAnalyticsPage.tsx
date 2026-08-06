import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import {
  useAdminUsers,
  useSetUserEnabled,
  useAdminFeedback,
  useAdminModelMetrics,
} from '../lib/admin';

export function AdminAnalyticsPage() {
  const { data: users } = useAdminUsers();
  const setEnabled = useSetUserEnabled();
  const { data: feedback } = useAdminFeedback();
  const { data: metrics } = useAdminModelMetrics();

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">Admin Analytics</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-900 dark:text-white">Monitoring &amp; management</h1>
        <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">
          Manage users, review model accuracy, and read user feedback.
        </p>
      </motion.section>

      {/* Model accuracy comparison */}
      <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">Model Accuracy</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">Random Forest vs. baseline</h2>
        {metrics ? (
          <div className="mt-4">
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Backend: <span className="font-medium">{metrics.backend}</span> · Train {metrics.trainSize} / Test {metrics.testSize}
            </p>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
              Dataset: <span className="font-medium">{metrics.datasetProfile?.name ?? 'Seed dataset'}</span>
              {metrics.datasetProfile?.sourceDatasetSize ? ` · Source size ${metrics.datasetProfile.sourceDatasetSize.toLocaleString()}` : ''}
              {metrics.emotionLabels?.length ? ` · Emotion labels ${metrics.emotionLabels.length}` : ''}
            </p>
            <div className="mt-4 space-y-3">
              {Object.entries(metrics.models).map(([key, model]) => (
                <div key={key} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-slate-900 dark:text-white">
                      {model.name} {model.deployed ? '· deployed' : ''}
                    </p>
                    <span className="text-sm font-semibold text-indigo-600 dark:text-indigo-300">
                      {(model.accuracy * 100).toFixed(1)}%
                    </span>
                  </div>
                  <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500"
                      style={{ width: `${Math.min(100, model.accuracy * 100)}%` }}
                    />
                  </div>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">F1 (macro): {(model.f1Macro * 100).toFixed(1)}%</p>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">Model metrics unavailable.</p>
        )}
      </section>

      {/* User management */}
      <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">User Management</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">Registered users</h2>
        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              <tr>
                <th className="px-3 py-2">Name</th>
                <th className="px-3 py-2">Email</th>
                <th className="px-3 py-2">Role</th>
                <th className="px-3 py-2">Status</th>
                <th className="px-3 py-2">Action</th>
              </tr>
            </thead>
            <tbody>
              {(users ?? []).map((user) => (
                <tr key={user.id} className="border-t border-slate-100 dark:border-slate-800">
                  <td className="px-3 py-3 text-slate-900 dark:text-white">{user.fullName}</td>
                  <td className="px-3 py-3 text-slate-600 dark:text-slate-300">{user.email}</td>
                  <td className="px-3 py-3 text-slate-600 dark:text-slate-300">{user.role}</td>
                  <td className="px-3 py-3">
                    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${user.enabled ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-200' : 'bg-rose-100 text-rose-700 dark:bg-rose-900/50 dark:text-rose-200'}`}>
                      {user.enabled ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td className="px-3 py-3">
                    <Button
                      size="sm"
                      variant={user.enabled ? 'danger' : 'primary'}
                      onClick={() => setEnabled.mutate({ id: user.id, enabled: !user.enabled })}
                    >
                      {user.enabled ? 'Disable' : 'Enable'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* User feedback */}
      <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">User Feedback</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">What users are saying</h2>
        <div className="mt-4 space-y-3">
          {(feedback ?? []).length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">No feedback submitted yet.</p>
          ) : (
            (feedback ?? []).map((item) => (
              <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium text-slate-900 dark:text-white">{item.userName}</p>
                  <span className="text-amber-400">{'★'.repeat(item.rating)}{'☆'.repeat(5 - item.rating)}</span>
                </div>
                {item.message ? <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{item.message}</p> : null}
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
