import { describe, expect, it } from 'vitest';
import { bandTone, confidenceLabel, deviationLabel } from './mindset';

describe('bandTone', () => {
  it('maps each band to a distinct tone', () => {
    expect(bandTone('Thriving')).toBe('positive');
    expect(bandTone('Steady')).toBe('neutral');
    expect(bandTone('Strained')).toBe('caution');
    expect(bandTone('Struggling')).toBe('concern');
  });

  it('falls back to unknown when there is no band', () => {
    expect(bandTone(null)).toBe('unknown');
  });
});

describe('confidenceLabel', () => {
  it('never shows a raw float to the user', () => {
    expect(confidenceLabel(0.95)).toBe('High confidence');
    expect(confidenceLabel(0.6)).toBe('Moderate confidence');
    expect(confidenceLabel(0.2)).toContain('Low confidence');
  });

  it('says plainly when there is nothing behind the number', () => {
    expect(confidenceLabel(0)).toBe('Not enough data');
  });
});

describe('deviationLabel', () => {
  it('says nothing without a deviation', () => {
    expect(deviationLabel(null)).toBeNull();
  });

  it('treats sub-sigma movement as normal rather than a finding', () => {
    expect(deviationLabel(0.4)).toBe('Within your usual range');
    expect(deviationLabel(-0.9)).toBe('Within your usual range');
  });

  it('names the direction once the movement is meaningful', () => {
    expect(deviationLabel(-1.4)).toBe('Below your usual range');
    expect(deviationLabel(1.4)).toBe('Above your usual range');
  });

  it('marks a large deviation more strongly', () => {
    expect(deviationLabel(-2.5)).toBe('Well below your usual range');
  });
});
