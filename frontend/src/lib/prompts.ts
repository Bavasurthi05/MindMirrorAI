/**
 * Rotating writing prompts.
 *
 * <p>A blank editor is the main reason entries never get written. The chosen prompt id is stored
 * with the entry so we can later see which prompts produce the richest signal.
 */
export interface JournalPrompt {
  id: string;
  text: string;
}

export const JOURNAL_PROMPTS: JournalPrompt[] = [
  { id: 'drained', text: 'What drained you today?' },
  { id: 'better-than-expected', text: 'What went better than you expected?' },
  { id: 'on-your-mind', text: "What's been sitting on your mind?" },
  { id: 'body', text: 'How has your body felt today — rest, energy, tension?' },
  { id: 'people', text: 'Who did you spend time with, and how did it leave you feeling?' },
  { id: 'tomorrow', text: 'What would make tomorrow a little easier?' },
  { id: 'grateful', text: 'Is there anything small you were glad about?' },
  { id: 'avoiding', text: 'Is there something you have been avoiding?' },
];

/**
 * A stable daily rotation: everyone gets the same prompts on a given day, and they change
 * day to day rather than shuffling on every render.
 */
export function promptsForToday(count = 3, today: Date = new Date()): JournalPrompt[] {
  const dayIndex = Math.floor(today.getTime() / 86_400_000);
  const size = JOURNAL_PROMPTS.length;
  const capped = Math.min(count, size);
  return Array.from({ length: capped }, (_, offset) => JOURNAL_PROMPTS[(dayIndex + offset) % size]);
}
