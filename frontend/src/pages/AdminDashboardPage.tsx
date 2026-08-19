import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line, Doughnut } from 'react-chartjs-2';
import { useState } from 'react';
import {
  ASSIGNABLE_ROLES,
  roleLabel,
  useAdminAuditLog,
  useAdminFeedback,
  useAdminModelMetrics,
  useAdminOverview,
  useAdminUsers,
  useSetUserEnabled,
  useSetUserRole,
  type AdminUser,
  type AssignableRole,
} from '../lib/admin';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../lib/api';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend);

const analyticsCardsFallback = [
  { label: 'Total Users', value: '—', detail: 'Registered accounts' },
  { label: 'Verified Users', value: '—', detail: 'Confirmed emails' },
  { label: 'Journal Entries', value: '—', detail: 'Across all users' },
  { label: 'Assessments', value: '—', detail: 'Completed check-ins' },
];

const DISTRIBUTION_COLORS = ['#6366f1', '#22d3ee', '#f59e0b', '#10b981', '#fb7185', '#a78bfa'];

export function AdminDashboardPage() {
  const { data: overview } = useAdminOverview();
  const { data: users = [] } = useAdminUsers();
  const { data: feedback = [] } = useAdminFeedback();
  const { data: modelMetrics } = useAdminModelMetrics();
  const setUserEnabled = useSetUserEnabled();
  const setUserRole = useSetUserRole();
  const { data: auditLog = [] } = useAdminAuditLog(10);
  const { user: currentUser } = useAuth();
  const [actionError, setActionError] = useState<string | null>(null);

  const currentEmail = currentUser?.email?.toLowerCase() ?? null;

  /** Granting or revoking admin is worth an explicit confirmation. */
  const handleRoleChange = async (target: AdminUser, role: AssignableRole) => {
    setActionError(null);
    if (role === target.role) return;

    const promoting = role === 'ROLE_ADMIN';
    const message = promoting
      ? `Make ${target.email} an administrator? They will be able to manage every account and deploy models.`
      : `Remove administrator rights from ${target.email}?`;
    if (!window.confirm(message)) return;

    try {
      await setUserRole.mutateAsync({ id: target.id, role });
    } catch (error) {
      // The 409s carry the reason (self-change, last admin) — show it as-is.
      setActionError(error instanceof ApiError ? error.message : 'Could not update that role.');
    }
  };

  const handleEnabledChange = async (target: AdminUser) => {
    setActionError(null);
    if (target.enabled && !window.confirm(`Disable ${target.email}? They will not be able to sign in.`)) {
      return;
    }
    try {
      await setUserEnabled.mutateAsync({ id: target.id, enabled: !target.enabled });
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Could not update that account.');
    }
  };

  const growth = overview?.userGrowth ?? [];
  const growthData = {
    labels: growth.map((point) => point.label),
    datasets: [
      {
        label: 'Total users',
        data: growth.map((point) => point.cumulativeUsers),
        borderColor: '#6366f1',
        backgroundColor: 'rgba(99, 102, 241, 0.15)',
        tension: 0.35,
        fill: true,
      },
    ],
  };

  const distribution = overview?.triggerDistribution ?? [];
  const triggerDistributionData = {
    labels: distribution.map((item) => item.category),
    datasets: [
      {
        data: distribution.map((item) => item.count),
        backgroundColor: distribution.map((_, i) => DISTRIBUTION_COLORS[i % DISTRIBUTION_COLORS.length]),
        borderWidth: 0,
      },
    ],
  };

  const analyticsCards = overview
    ? [
        { label: 'Total Users', value: `${overview.totalUsers}`, detail: `${overview.verifiedUsers} verified` },
        { label: 'Journal Entries', value: `${overview.totalJournalEntries}`, detail: 'Across all users' },
        { label: 'Mood Check-ins', value: `${overview.totalMoodEntries}`, detail: 'Logged moods' },
        { label: 'Assessments', value: `${overview.totalAssessments}`, detail: 'Completed questionnaires' },
      ]
    : analyticsCardsFallback;

  const modelRows = modelMetrics?.models
    ? Object.entries(modelMetrics.models).map(([key, value]) => ({ key, ...value }))
    : [];

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-sm"
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-300">Admin Dashboard</p>
            <h1 className="mt-2 text-3xl font-semibold">Administrative oversight for the platform</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-300">
              Platform-wide sign-ups, engagement and trigger trends, plus user management, model
              accuracy and feedback review.
            </p>
          </div>
          <Link
            to="/admin/analytics"
            className="rounded-2xl border border-white/10 bg-white/10 px-4 py-3 text-sm text-slate-200 backdrop-blur transition hover:bg-white/20"
          >
            <p className="font-semibold text-white">Models &amp; Feedback →</p>
            <p>Retraining, quality gate, prediction feedback</p>
          </Link>
        </div>
      </motion.section>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {analyticsCards.map((card) => (
          <motion.div
            key={card.label}
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
            className="rounded-[1.4rem] border border-slate-200 bg-white p-5 shadow-sm"
          >
            <p className="text-sm text-slate-500">{card.label}</p>
            <p className="mt-3 text-2xl font-semibold text-slate-900">{card.value}</p>
            <p className="mt-2 text-sm text-slate-600">{card.detail}</p>
          </motion.div>
        ))}
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.05 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">User Growth</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Adoption over time</h2>
          <div className="mt-6">
            {growth.length === 0 ? (
              <p className="text-sm text-slate-500">No sign-up history yet.</p>
            ) : (
              <Line
                data={growthData}
                options={{
                  plugins: { legend: { display: false } },
                  scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
                }}
              />
            )}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.08 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Wellness Statistics</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Platform wellbeing health</h2>
          <div className="mt-6 mx-auto max-w-sm">
            {distribution.length === 0 ? (
              <p className="text-sm text-slate-500">No triggers logged across the platform yet.</p>
            ) : (
              <Doughnut data={triggerDistributionData} options={{ plugins: { legend: { position: 'bottom' } } }} />
            )}
          </div>
        </motion.section>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.1 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">User management</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Accounts and access</h2>
          <p className="mt-2 text-sm text-slate-500">
            Administrators can manage every account and deploy models. You cannot change your own
            role or disable your own account, and the last active administrator cannot be removed.
          </p>

          {actionError ? (
            <p className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{actionError}</p>
          ) : null}

          <div className="mt-6 space-y-3">
            {users.map((user) => {
              const isSelf = currentEmail !== null && user.email.toLowerCase() === currentEmail;
              const isAdmin = user.role === 'ROLE_ADMIN';
              return (
                <div key={user.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-semibold text-slate-900">
                        {user.fullName}
                        {isSelf ? (
                          <span className="ml-2 rounded-full bg-indigo-100 px-2 py-0.5 text-xs font-semibold text-indigo-700">
                            You
                          </span>
                        ) : null}
                      </p>
                      <p className="truncate text-sm text-slate-600">{user.email}</p>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                      <label className="sr-only" htmlFor={`role-${user.id}`}>
                        Role for {user.email}
                      </label>
                      <select
                        id={`role-${user.id}`}
                        value={user.role}
                        // Self-changes are blocked in the UI as well as the API, so the
                        // control never invites an action that is guaranteed to fail.
                        disabled={isSelf || setUserRole.isPending}
                        onChange={(event) => handleRoleChange(user, event.target.value as AssignableRole)}
                        title={isSelf ? 'You cannot change your own role' : undefined}
                        className={`rounded-full border px-3 py-1 text-xs font-semibold ${
                          isAdmin
                            ? 'border-indigo-200 bg-indigo-50 text-indigo-700'
                            : 'border-slate-200 bg-white text-slate-700'
                        } disabled:cursor-not-allowed disabled:opacity-60`}
                      >
                        {ASSIGNABLE_ROLES.map((role) => (
                          <option key={role} value={role}>
                            {roleLabel(role)}
                          </option>
                        ))}
                      </select>

                      <button
                        type="button"
                        disabled={isSelf || setUserEnabled.isPending}
                        title={isSelf ? 'You cannot disable your own account' : undefined}
                        onClick={() => handleEnabledChange(user)}
                        className={`rounded-full px-3 py-1 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60 ${
                          user.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
                        }`}
                      >
                        {user.enabled ? 'Enabled' : 'Disabled'}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, delay: 0.12 }}
          className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
        >
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Model accuracy</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">Random Forest vs baseline</h2>
          <div className="mt-6 space-y-3">
            {modelRows.map((row) => (
              <div key={row.key} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-center justify-between">
                  <p className="font-semibold text-slate-900">{row.name}</p>
                  <span className="text-sm text-slate-600">{row.accuracy.toFixed(2)} accuracy</span>
                </div>
                <p className="mt-1 text-sm text-slate-600">F1 macro: {row.f1Macro.toFixed(2)}</p>
              </div>
            ))}
          </div>
        </motion.section>
      </div>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.13 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Audit log</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900">Recent account changes</h2>
        <p className="mt-2 text-sm text-slate-500">
          Role and access changes are recorded so they can be explained later.
        </p>
        <div className="mt-6 space-y-2">
          {auditLog.length === 0 ? (
            <p className="text-sm text-slate-500">No account changes recorded yet.</p>
          ) : (
            auditLog.map((entry) => (
              <div
                key={entry.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-2xl bg-slate-50 px-4 py-3 text-sm"
              >
                <span className="text-slate-700">
                  <span className="font-medium text-slate-900">{entry.actorEmail}</span>
                  {entry.action === 'ROLE_CHANGED' ? ' changed the role of ' : ' set '}
                  <span className="font-medium text-slate-900">{entry.targetEmail}</span>
                  {entry.action === 'ROLE_CHANGED'
                    ? ` from ${roleLabel(entry.previousValue ?? '')} to ${roleLabel(entry.newValue ?? '')}`
                    : ` to ${entry.newValue}`}
                </span>
                <span className="text-xs text-slate-500">
                  {new Date(entry.createdAt).toLocaleString()}
                </span>
              </div>
            ))
          )}
        </div>
      </motion.section>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.14 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Feedback review</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900">Recent user submissions</h2>
        <div className="mt-6 space-y-3">
          {feedback.map((item) => (
            <div key={item.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-slate-900">{item.userName}</p>
                  <p className="text-sm text-slate-600">{item.userEmail}</p>
                </div>
                <span className="rounded-full bg-amber-100 px-3 py-1 text-sm font-semibold text-amber-700">{item.rating}/5</span>
              </div>
              <p className="mt-3 text-sm leading-7 text-slate-700">{item.message}</p>
            </div>
          ))}
        </div>
      </motion.section>
    </div>
  );
}
