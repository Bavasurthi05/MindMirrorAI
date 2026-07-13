import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';

const moodOptions = [
  { value: 'calm', label: 'Calm', emoji: '🌿' },
  { value: 'reflective', label: 'Reflective', emoji: '🪞' },
  { value: 'energized', label: 'Energized', emoji: '⚡' },
  { value: 'overwhelmed', label: 'Overwhelmed', emoji: '🌧️' },
];

const previousEntries = [
  {
    title: 'Evening reset after a long day',
    date: 'Jul 10, 2026',
    mood: 'Reflective',
    excerpt: 'I noticed how a few minutes of silence helped me settle before sleep.',
  },
  {
    title: 'A grounded morning routine',
    date: 'Jul 08, 2026',
    mood: 'Calm',
    excerpt: 'The simple act of stretching and journaling made the day feel lighter.',
  },
  {
    title: 'A burst of clarity after work',
    date: 'Jul 06, 2026',
    mood: 'Energized',
    excerpt: 'I felt more focused once I stepped away from the screen and took a walk.',
  },
];

export function JournalPage() {
  const [content, setContent] = useState('');
  const [mood, setMood] = useState('calm');
  const [title, setTitle] = useState('');

  const stats = useMemo(() => {
    const words = content.trim().split(/\s+/).filter(Boolean).length;
    const characters = content.length;
    return { words, characters };
  }, [content]);

  const handleDraft = () => {
    // Mock-only action: no API call.
    alert('Draft saved locally for this preview.');
  };

  const handleSubmit = () => {
    alert('Entry submitted for preview. No backend connection is involved.');
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
              Capture your thoughts in a calm editor with mood-aware context and mock persistence for the UI preview.
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
                <Button variant="secondary" onClick={handleDraft}>
                  Save draft
                </Button>
                <Button onClick={handleSubmit}>Submit</Button>
              </div>
            </div>
          </div>

          <div className="rounded-[1.5rem] border border-slate-200 bg-slate-50 p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Previous Journal</p>
                <h2 className="mt-2 text-xl font-semibold text-slate-900">Recent entries</h2>
              </div>
            </div>

            <div className="mt-5 space-y-3">
              {previousEntries.map((entry) => (
                <div key={entry.title} className="rounded-2xl border border-slate-200 bg-white p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-semibold text-slate-900">{entry.title}</p>
                    <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
                      {entry.mood}
                    </span>
                  </div>
                  <p className="mt-2 text-sm text-slate-500">{entry.date}</p>
                  <p className="mt-2 text-sm leading-6 text-slate-600">{entry.excerpt}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </motion.section>
    </div>
  );
}
