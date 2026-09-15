import { describe, expect, it } from 'vitest';
import {
  MAX_UPLOAD_BYTES,
  PROVIDER_GUIDES,
  importProgress,
  providerLabel,
  skippedSummary,
  validateExportFile,
} from './social-imports';

const noSkips = { skippedDuplicates: 0, skippedReposts: 0, skippedShort: 0, skippedOverLimit: 0 };

describe('validateExportFile', () => {
  it('asks for a file when none is chosen', () => {
    expect(validateExportFile(null)).toContain('Choose a file');
  });

  it('accepts the archive and the extracted posts files, in any case', () => {
    expect(validateExportFile({ name: 'twitter-archive.ZIP', size: 1024 })).toBeNull();
    expect(validateExportFile({ name: 'posts_1.json', size: 1024 })).toBeNull();
    expect(validateExportFile({ name: 'tweets.js', size: 1024 })).toBeNull();
  });

  it('rejects files that cannot be an export', () => {
    expect(validateExportFile({ name: 'screenshot.png', size: 1024 })).toContain('.zip');
  });

  it('rejects an empty file', () => {
    expect(validateExportFile({ name: 'tweets.js', size: 0 })).toContain('empty');
  });

  it('rejects files over the upload limit before sending them', () => {
    expect(validateExportFile({ name: 'archive.zip', size: MAX_UPLOAD_BYTES + 1 })).toContain('100 MB');
    expect(validateExportFile({ name: 'archive.zip', size: MAX_UPLOAD_BYTES })).toBeNull();
  });
});

describe('importProgress', () => {
  it('is complete when there was nothing to analyze', () => {
    expect(importProgress({ postsImported: 0, postsAnalyzed: 0, postsFailed: 0 })).toBe(100);
  });

  it('counts failed analyses as processed, since they no longer block progress', () => {
    expect(importProgress({ postsImported: 200, postsAnalyzed: 90, postsFailed: 10 })).toBe(50);
  });

  it('never exceeds 100', () => {
    expect(importProgress({ postsImported: 10, postsAnalyzed: 12, postsFailed: 0 })).toBe(100);
  });
});

describe('skippedSummary', () => {
  it('says nothing when nothing was skipped', () => {
    expect(skippedSummary(noSkips)).toEqual([]);
  });

  it('explains each reason, singular and plural', () => {
    const lines = skippedSummary({ skippedDuplicates: 1, skippedReposts: 3, skippedShort: 1, skippedOverLimit: 250 });

    expect(lines).toEqual([
      '1 post was already imported earlier',
      "3 reposts of other people's posts left out",
      '1 post with no text, or too short to analyze',
      '250 older posts beyond the 1,000-post limit',
    ]);
  });

  it('only mentions the reasons that apply', () => {
    expect(skippedSummary({ ...noSkips, skippedReposts: 2 })).toEqual(["2 reposts of other people's posts left out"]);
  });
});

describe('provider guides', () => {
  it('cover exactly the platforms the API accepts', () => {
    expect(PROVIDER_GUIDES.map((guide) => guide.id)).toEqual(['X', 'INSTAGRAM', 'FACEBOOK']);
  });

  it('give every platform steps and upload guidance', () => {
    for (const guide of PROVIDER_GUIDES) {
      expect(guide.steps.length).toBeGreaterThan(0);
      expect(guide.upload.length).toBeGreaterThan(0);
    }
  });

  it('label providers for display', () => {
    expect(providerLabel('X')).toBe('X (Twitter)');
    expect(providerLabel('INSTAGRAM')).toBe('Instagram');
  });
});
