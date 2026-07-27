interface HeatmapCalendarProps {
  cells: { date: string; score: number | null }[];
}

export function heatmapColor(score: number | null): string {
  if (score === null) return 'bg-slate-100';
  if (score >= 80) return 'bg-emerald-500';
  if (score >= 65) return 'bg-emerald-400';
  if (score >= 50) return 'bg-amber-400';
  if (score >= 35) return 'bg-orange-400';
  return 'bg-rose-500';
}

/**
 * Wellness heatmap calendar — one cell per day, coloured green (high) to red (low).
 * Days without data render as neutral slate.
 */
export function HeatmapCalendar({ cells }: HeatmapCalendarProps) {
  return (
    <div>
      <div className="grid grid-flow-col grid-rows-7 gap-1.5" style={{ gridAutoColumns: 'minmax(0, 1fr)' }}>
        {cells.map((cell) => (
          <div
            key={cell.date}
            title={cell.score === null ? `${cell.date}: no check-in` : `${cell.date}: ${cell.score}/100`}
            className={`aspect-square rounded-[4px] ${heatmapColor(cell.score)}`}
          />
        ))}
      </div>
      <div className="mt-3 flex items-center gap-2 text-xs text-slate-500">
        <span>Low</span>
        <span className="h-3 w-3 rounded-[3px] bg-rose-500" />
        <span className="h-3 w-3 rounded-[3px] bg-orange-400" />
        <span className="h-3 w-3 rounded-[3px] bg-amber-400" />
        <span className="h-3 w-3 rounded-[3px] bg-emerald-400" />
        <span className="h-3 w-3 rounded-[3px] bg-emerald-500" />
        <span>High</span>
      </div>
    </div>
  );
}
