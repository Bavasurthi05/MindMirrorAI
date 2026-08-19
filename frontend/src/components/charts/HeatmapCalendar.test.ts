import { describe, expect, it } from 'vitest';
import { buildHeatmapLayout, heatmapColor, weekdayIndex } from './HeatmapCalendar';

function range(start: string, days: number, score: number | null = 50) {
  const cells = [];
  const begin = new Date(`${start}T00:00:00Z`);
  for (let i = 0; i < days; i++) {
    const day = new Date(begin);
    day.setUTCDate(begin.getUTCDate() + i);
    cells.push({ date: day.toISOString().slice(0, 10), score });
  }
  return cells;
}

describe('heatmapColor', () => {
  it('keeps days with no data neutral rather than reading as a low score', () => {
    expect(heatmapColor(null)).toContain('bg-slate-100');
  });

  it('maps high scores to green', () => {
    expect(heatmapColor(85)).toBe('bg-emerald-500');
    expect(heatmapColor(70)).toBe('bg-emerald-400');
  });

  it('maps mid scores to amber/orange', () => {
    expect(heatmapColor(55)).toBe('bg-amber-400');
    expect(heatmapColor(40)).toBe('bg-orange-400');
  });

  it('maps low scores to red', () => {
    expect(heatmapColor(20)).toBe('bg-rose-500');
  });
});

describe('weekdayIndex', () => {
  it('puts Monday first and Sunday last', () => {
    // 2026-08-17 is a Monday.
    expect(weekdayIndex('2026-08-17')).toBe(0);
    expect(weekdayIndex('2026-08-18')).toBe(1);
    expect(weekdayIndex('2026-08-23')).toBe(6);
  });
});

describe('buildHeatmapLayout', () => {
  it('has nothing to lay out with no cells', () => {
    expect(buildHeatmapLayout([])).toEqual({ weeks: [], monthLabels: [] });
  });

  it('pads the first column so each row is a fixed weekday', () => {
    // Starts on a Wednesday, so Mon and Tue of that week must be blank.
    const layout = buildHeatmapLayout(range('2026-08-19', 7));

    expect(layout.weeks[0][0]).toBeNull();
    expect(layout.weeks[0][1]).toBeNull();
    expect(layout.weeks[0][2]?.date).toBe('2026-08-19');
  });

  it('keeps every column exactly seven tall, padding the final week', () => {
    const layout = buildHeatmapLayout(range('2026-08-19', 10));

    expect(layout.weeks.every((week) => week.length === 7)).toBe(true);
    expect(layout.weeks[layout.weeks.length - 1].filter(Boolean).length).toBeLessThan(7);
  });

  it('places every day in the row matching its weekday', () => {
    const layout = buildHeatmapLayout(range('2026-07-16', 35));

    for (const week of layout.weeks) {
      for (let row = 0; row < 7; row++) {
        const cell = week[row];
        if (cell) {
          expect(weekdayIndex(cell.date)).toBe(row);
        }
      }
    }
  });

  it('loses no days', () => {
    const cells = range('2026-07-16', 35);
    const layout = buildHeatmapLayout(cells);
    const placed = layout.weeks.flat().filter(Boolean);

    expect(placed).toHaveLength(35);
    expect(placed.map((cell) => cell!.date)).toEqual(cells.map((cell) => cell.date));
  });

  it('starts a Monday range with no padding', () => {
    const layout = buildHeatmapLayout(range('2026-08-17', 7));

    expect(layout.weeks).toHaveLength(1);
    expect(layout.weeks[0][0]?.date).toBe('2026-08-17');
  });

  it('labels a column only when the month changes', () => {
    const layout = buildHeatmapLayout(range('2026-07-16', 35));

    expect(layout.monthLabels[0]).toBe('Jul');
    // The next label appears once, where August begins — not on every column.
    expect(layout.monthLabels.filter((label) => label === 'Jul')).toHaveLength(1);
    expect(layout.monthLabels.filter((label) => label === 'Aug')).toHaveLength(1);
  });
});
