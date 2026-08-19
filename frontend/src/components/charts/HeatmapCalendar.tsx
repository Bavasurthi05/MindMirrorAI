export interface HeatCell {
  date: string;
  score: number | null;
}

interface HeatmapCalendarProps {
  cells: HeatCell[];
}

/** Weekday rows, Monday first. Index matches `weekdayIndex`. */
const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export function heatmapColor(score: number | null): string {
  if (score === null) return 'bg-slate-100 dark:bg-slate-800';
  if (score >= 80) return 'bg-emerald-500';
  if (score >= 65) return 'bg-emerald-400';
  if (score >= 50) return 'bg-amber-400';
  if (score >= 35) return 'bg-orange-400';
  return 'bg-rose-500';
}

/** 0 = Monday … 6 = Sunday. `Date.getUTCDay()` is 0 = Sunday, so shift it. */
export function weekdayIndex(isoDate: string): number {
  const date = new Date(`${isoDate}T00:00:00Z`);
  return (date.getUTCDay() + 6) % 7;
}

export interface HeatmapLayout {
  /** Column-major weeks; each week has exactly 7 slots, null where no cell belongs. */
  weeks: (HeatCell | null)[][];
  /** Month label per week column, or null when it repeats the previous column. */
  monthLabels: (string | null)[];
}

/**
 * Arranges a flat run of days into calendar columns.
 *
 * <p>Each column is one week and each row a fixed weekday, so the grid reads like a calendar.
 * The first column is padded with nulls when the range does not begin on a Monday — without
 * that padding the rows drift and a "Tuesday" row is only accidentally Tuesdays.
 */
export function buildHeatmapLayout(cells: HeatCell[]): HeatmapLayout {
  if (cells.length === 0) {
    return { weeks: [], monthLabels: [] };
  }

  const weeks: (HeatCell | null)[][] = [];
  let current: (HeatCell | null)[] = new Array(weekdayIndex(cells[0].date)).fill(null);

  for (const cell of cells) {
    current.push(cell);
    if (current.length === 7) {
      weeks.push(current);
      current = [];
    }
  }
  if (current.length > 0) {
    weeks.push([...current, ...new Array(7 - current.length).fill(null)]);
  }

  // Label a column only when its month differs from the column before it.
  let previousMonth = -1;
  const monthLabels = weeks.map((week) => {
    const first = week.find((cell): cell is HeatCell => cell !== null);
    if (!first) return null;
    const month = new Date(`${first.date}T00:00:00Z`).getUTCMonth();
    if (month === previousMonth) return null;
    previousMonth = month;
    return MONTHS[month];
  });

  return { weeks, monthLabels };
}

function formatTooltip(cell: HeatCell): string {
  const date = new Date(`${cell.date}T00:00:00Z`);
  const label = `${WEEKDAYS[weekdayIndex(cell.date)]} ${date.getUTCDate()} ${MONTHS[date.getUTCMonth()]}`;
  return cell.score === null ? `${label} — no check-in` : `${label} — ${cell.score}/100`;
}

/**
 * Wellness heatmap calendar: one small cell per day, weeks as columns, coloured green (high)
 * to red (low). Days without data stay neutral rather than reading as a low score.
 */
export function HeatmapCalendar({ cells }: HeatmapCalendarProps) {
  if (cells.length === 0) {
    return (
      <p className="text-sm text-slate-500 dark:text-slate-400">
        No wellness history yet — check in or write a reflection and this fills in day by day.
      </p>
    );
  }

  const { weeks, monthLabels } = buildHeatmapLayout(cells);
  const recorded = cells.filter((cell) => cell.score !== null).length;

  return (
    <div>
      <div className="overflow-x-auto">
        <div className="inline-block min-w-full">
          <div className="flex gap-1 pl-9 text-[10px] text-slate-400">
            {monthLabels.map((label, index) => (
              <span key={index} className="w-4 shrink-0">
                {label ?? ''}
              </span>
            ))}
          </div>

          <div className="mt-1 flex gap-1">
            <div className="flex w-8 shrink-0 flex-col gap-1 text-[10px] leading-4 text-slate-400">
              {/* Only alternate weekdays are labelled; all seven would not fit legibly. */}
              {WEEKDAYS.map((day, index) => (
                <span key={day} className="h-4">
                  {index % 2 === 0 ? day : ''}
                </span>
              ))}
            </div>

            {weeks.map((week, weekIndex) => (
              <div key={weekIndex} className="flex shrink-0 flex-col gap-1">
                {week.map((cell, dayIndex) =>
                  cell === null ? (
                    <div key={dayIndex} className="h-4 w-4" />
                  ) : (
                    <div
                      key={cell.date}
                      title={formatTooltip(cell)}
                      className={`h-4 w-4 rounded-[3px] ${heatmapColor(cell.score)}`}
                    />
                  ),
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500 dark:text-slate-400">
        <span>
          {recorded} day{recorded === 1 ? '' : 's'} with data of {cells.length}
        </span>
        <div className="flex items-center gap-1.5">
          <span>Lower</span>
          <span className="h-3 w-3 rounded-[3px] bg-rose-500" />
          <span className="h-3 w-3 rounded-[3px] bg-orange-400" />
          <span className="h-3 w-3 rounded-[3px] bg-amber-400" />
          <span className="h-3 w-3 rounded-[3px] bg-emerald-400" />
          <span className="h-3 w-3 rounded-[3px] bg-emerald-500" />
          <span>Higher</span>
          <span className="ml-2 flex items-center gap-1">
            <span className="h-3 w-3 rounded-[3px] bg-slate-100 dark:bg-slate-800" />
            No data
          </span>
        </div>
      </div>
    </div>
  );
}
