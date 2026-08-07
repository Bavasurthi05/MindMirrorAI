import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import { useChangePassword, useProfile, useUpdateProfile } from '../lib/profile';
import { useGoals, useCreateGoal, useIncrementGoal, useDeleteGoal } from '../lib/goals';

export function ProfilePage() {
  const { data: profile, isLoading } = useProfile();
  const { data: goals } = useGoals();
  const updateProfile = useUpdateProfile();
  const changePassword = useChangePassword();
  const createGoal = useCreateGoal();
  const incrementGoal = useIncrementGoal();
  const deleteGoal = useDeleteGoal();
  const [newGoal, setNewGoal] = useState('');
  const [target, setTarget] = useState(5);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  useEffect(() => {
    if (profile) {
      setFirstName(profile.firstName ?? '');
      setLastName(profile.lastName ?? '');
    }
  }, [profile]);

  const handleAddGoal = async () => {
    if (!newGoal.trim()) return;
    await createGoal.mutateAsync({ title: newGoal.trim(), target });
    setNewGoal('');
    setTarget(5);
  };

  const handleSaveProfile = async () => {
    setStatusMessage(null);
    try {
      await updateProfile.mutateAsync({ firstName: firstName.trim(), lastName: lastName.trim() });
      setStatusMessage('Profile updated successfully.');
    } catch {
      setStatusMessage('Unable to update profile right now.');
    }
  };

  const handlePasswordChange = async () => {
    setStatusMessage(null);
    try {
      await changePassword.mutateAsync({ currentPassword, newPassword });
      setCurrentPassword('');
      setNewPassword('');
      setStatusMessage('Password updated successfully.');
    } catch {
      setStatusMessage('Current password is incorrect or the request failed.');
    }
  };

  const stats = [
    { label: 'Journal streak', value: profile ? `${profile.journalStreak} day${profile.journalStreak === 1 ? '' : 's'}` : '—' },
    { label: 'Journal entries', value: profile?.journalCount ?? '—' },
    { label: 'Mood check-ins', value: profile?.moodCount ?? '—' },
    { label: 'Goals completed', value: profile ? `${profile.goalsCompleted}/${profile.goalsTotal}` : '—' },
  ];

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">Profile</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-900 dark:text-white">
          {isLoading ? 'Loading…' : profile?.fullName}
        </h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{profile?.email}</p>
        <div className="mt-2 flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-indigo-100 px-3 py-1 font-medium text-indigo-700 dark:bg-indigo-900/50 dark:text-indigo-200">
            {profile?.role ?? 'ROLE_USER'}
          </span>
          <span className={`rounded-full px-3 py-1 font-medium ${profile?.emailVerified ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-200' : 'bg-amber-100 text-amber-700'}`}>
            {profile?.emailVerified ? 'Email verified' : 'Email unverified'}
          </span>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {stats.map((stat) => (
            <div key={stat.label} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800">
              <p className="text-sm text-slate-500 dark:text-slate-400">{stat.label}</p>
              <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-white">{stat.value}</p>
            </div>
          ))}
        </div>

        <div className="mt-6 grid gap-4 lg:grid-cols-2">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800">
            <p className="text-sm font-semibold text-slate-900 dark:text-white">Edit profile</p>
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              <input value={firstName} onChange={(event) => setFirstName(event.target.value)} placeholder="First name" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-900" />
              <input value={lastName} onChange={(event) => setLastName(event.target.value)} placeholder="Last name" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-900" />
            </div>
            <div className="mt-3 flex items-center gap-3">
              <Button onClick={handleSaveProfile} disabled={updateProfile.isPending}>Save</Button>
              {statusMessage ? <span className="text-sm text-emerald-600">{statusMessage}</span> : null}
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800">
            <p className="text-sm font-semibold text-slate-900 dark:text-white">Change password</p>
            <div className="mt-3 space-y-3">
              <input type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} placeholder="Current password" className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-900" />
              <input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} placeholder="New password" className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-900" />
            </div>
            <div className="mt-3">
              <Button variant="secondary" onClick={handlePasswordChange} disabled={changePassword.isPending}>Update password</Button>
            </div>
          </div>
        </div>
      </motion.section>

      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, delay: 0.05 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600 dark:text-cyan-400">Weekly Wellness Goals</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-white">Track your intentions</h2>

        <div className="mt-6 space-y-3">
          {(goals ?? []).map((goal) => (
            <div key={goal.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-slate-900 dark:text-white">{goal.title}</p>
                <span className={`text-xs font-semibold ${goal.completed ? 'text-emerald-600' : 'text-slate-500 dark:text-slate-400'}`}>
                  {goal.progress}/{goal.target}
                </span>
              </div>
              <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500"
                  style={{ width: `${Math.min(100, (goal.progress / goal.target) * 100)}%` }}
                />
              </div>
              <div className="mt-3 flex gap-2">
                <Button size="sm" onClick={() => incrementGoal.mutate(goal.id)} disabled={goal.completed}>
                  Log progress
                </Button>
                <Button size="sm" variant="ghost" onClick={() => deleteGoal.mutate(goal.id)}>
                  Remove
                </Button>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
          <input
            value={newGoal}
            onChange={(event) => setNewGoal(event.target.value)}
            placeholder="Add a new goal…"
            className="flex-1 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
          />
          <input
            type="number"
            min={1}
            value={target}
            onChange={(event) => setTarget(Math.max(1, Number(event.target.value)))}
            className="w-24 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
          />
          <Button onClick={handleAddGoal} disabled={createGoal.isPending}>
            {createGoal.isPending ? 'Adding…' : 'Add goal'}
          </Button>
        </div>
      </motion.section>
    </div>
  );
}
