interface EmotionTimelineProps {
  points: { date: string; label: string; score: number }[];
}

const EMOJI_BY_LABEL: Record<string, string> = {
  joy: '😊',
  happy: '😊',
  calm: '😌',
  neutral: '😐',
  sadness: '😔',
  sad: '😔',
  anger: '😠',
  fear: '😨',
  anxiety: '😰',
  fatigue: '😪',
  stress: '😣',
};

function emojiFor(label: string): string {
  return EMOJI_BY_LABEL[label.toLowerCase()] ?? '😐';
}

/**
 * Horizontal emotion timeline — recent check-ins as emoji with their date and score.
 */
export function EmotionTimeline({ points }: EmotionTimelineProps) {
  if (points.length === 0) {
    return <p className="text-sm text-slate-500">No mood check-ins yet.</p>;
  }

  return (
    <div className="flex items-end gap-4 overflow-x-auto pb-2">
      {points.map((point) => (
        <div key={`${point.date}-${point.score}`} className="flex min-w-[72px] flex-col items-center rounded-2xl border border-slate-200 bg-slate-50 px-3 py-3">
          <span className="text-3xl" title={point.label}>
            {emojiFor(point.label)}
          </span>
          <span className="mt-1 text-xs font-semibold capitalize text-slate-700">{point.label}</span>
          <span className="mt-1 text-[11px] text-slate-400">{point.date.slice(5)}</span>
          <span className="mt-1 text-[11px] font-medium text-cyan-600">{point.score}/100</span>
        </div>
      ))}
    </div>
  );
}
