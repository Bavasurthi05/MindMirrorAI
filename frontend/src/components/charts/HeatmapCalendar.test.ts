import { describe, expect, it } from 'vitest';
import { heatmapColor } from './HeatmapCalendar';

describe('heatmapColor', () => {
  it('returns neutral for no data', () => {
    expect(heatmapColor(null)).toBe('bg-slate-100');
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
