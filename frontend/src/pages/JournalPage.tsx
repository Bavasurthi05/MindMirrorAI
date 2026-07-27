import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import { useCreateJournalEntry, useJournalEntries } from '../lib/journal';
import { ApiError } from '../lib/api';

const moodOptions = [
  { value: 'calm', label: 'Calm', emoji: '🌿' },
  { value: 'reflective', label: 'Reflective', emoji: '🪞' },
  { value: 'energized', label: 'Energized', emoji: '⚡' },
  { value: 'overwhelmed', label: 'Overwhelmed', emoji: '🌧️' },
];

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: '2-digit',
  year: 'numeric',
});

export function JournalPage() {
  const [content, setContent] = useState('');
  const [mood, setMood] = useState('calm');
  const [title, setTitle] = useState('');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const { data: entriesPage, isLoading: isLoadingEntries } = useJournalEntries(0, 10);
  const createEntry = useCreateJournalEntry();

  const stats = useMemo(() => {
    const words = content.trim().split(/\s+/).filter(Boolean).length;
    const characters = content.length;
    return { words, characters };
  }, [content]);

  const resetForm = () => {
    setTitle('');
    setContent('');
    setMood('calm');
  };

  const handleDraft = () => {
    setFeedback({ type: 'success', text: 'Draft kept in this editor. Submit to save it to your journal.' });
  };

  const handleSubmit = async () => {
    setFeedback(null);
    if (!title.trim() || !content.trim()) {
      setFeedback({ type: 'error', text: 'Please add a title and some content before submitting.' });
      return;
    }

    try {
      await createEntry.mutateAsync({ title: title.trim(), content: content.trim(), mood });
      resetForm();
      setFeedback({ type: 'success', text: 'Your journal entry has been saved.' });
    } catch (error) {
      const text = error instanceof ApiError ? error.message : 'Unable to save your entry. Please try again.';
      setFeedback({ type: 'error', text });
    }
  };

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Journal</p>
            <h1 className="mt-2 text-3xl font-semibold text-slate-900">Write your reflections</h1>
            <p className="mt-2 max-w-2xl text-sm leading-7 text-slate-600">
              Capture your thoughts in a calm editor with mood-aware context. Entries are saved securely to your account.
            </p>
          </div>
          <div className="flex flex-wrap gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
            <span>Characters: {stats.characters}</span>
            <span>•</span>
            <span>Words: {stats.words}</span>
          </div>
        </div>

        <div className="mt-6 grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="space-y-4">
            <label className="block text-sm font-medium text-slate-700">
              Entry title
              <input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="What felt important today?"
                className="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
              />
            </label>

            <label className="block text-sm font-medium text-slate-700">
              Mood
              <div className="mt-2 flex flex-wrap gap-2">
                {moodOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setMood(option.value)}
                    className={`rounded-full border px-3 py-2 text-sm transition ${
                      mood === option.value
                        ? 'border-indigo-500 bg-indigo-600 text-white'
                        : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    {option.emoji} {option.label}
                  </button>
                ))}
              </div>
            </label>

            <label className="block text-sm font-medium text-slate-700">
              Journal entry
              <textarea
                value={content}
                onChange={(event) => setContent(event.target.value)}
                rows={14}
                placeholder="Write freely about your day, your mood, and what matters most right now..."
                className="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm leading-7 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
              />
            </label>

            <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
              <div>
                <p className="font-medium text-slate-900">Character counter</p>
                <p>{stats.characters} characters</p>
              </div>
              <div>
                <p className="font-medium text-slate-900">Word counter</p>
                <p>{stats.words} words</p>
              </div>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={handleDraft} type="button">
                  Save draft
                </Button>
                <Button onClick={handleSubmit} type="button" disabled={createEntry.isPending}>
                  {createEntry.isPending ? 'Saving…' : 'Submit'}
                </Button>
              </div>
            </div>

            {feedback ? (
              <p
                className={`rounded-2xl px-4 py-3 text-sm ${
                  feedback.type === 'success'
                    ? 'bg-emerald-50 text-emerald-700'
                    : 'bg-rose-50 text-rose-700'
                }`}
              >
                {feedback.text}
              </p>
            ) : null}
          </div>

          <div className="rounded-[1.5rem] border border-slate-200 bg-slate-50 p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Previous Journal</p>
                <h2 className="mt-2 text-xl font-semibold text-slate-900">Recent entries</h2>
              </div>
            </div>

            <div className="mt-5 space-y-3">
              {isLoadingEntries ? (
                <p className="text-sm text-slate-500">Loading your entries…</p>
              ) : entriesPage && entriesPage.content.length > 0 ? (
                entriesPage.content.map((entry) => (
                  <div key={entry.id} className="rounded-2xl border border-slate-200 bg-white p-4">
                    <div className="flex items-center justify-between gap-3">
                      <p className="font-semibold text-slate-900">{entry.title}</p>
                      {entry.mood ? (
                        <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
                          {entry.mood}
                        </span>
                      ) : null}
                    </div>
                    <p className="mt-2 text-sm text-slate-500">
                      {dateFormatter.format(new Date(entry.createdAt))}
                    </p>
                    <p className="mt-2 text-sm leading-6 text-slate-600">
                      {entry.content.length > 160 ? `${entry.content.slice(0, 160)}…` : entry.content}
                    </p>
                  </div>
                ))
              ) : (
                <p className="text-sm text-slate-500">
                  No entries yet. Write your first reflection to see it here.
                </p>
              )}
            </div>
          </div>
        </div>
      </motion.section>
    </div>
  );
}
