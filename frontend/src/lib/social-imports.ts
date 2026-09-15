import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

/**
 * Social media posts come from the user's own data export rather than a live account
 * connection: the platforms' API terms rule out mental-health analysis of their data.
 */

export type SocialProvider = 'X' | 'INSTAGRAM' | 'FACEBOOK';

export interface SocialImport {
  id: number;
  provider: SocialProvider;
  originalFilename: string | null;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  /** Posts found in the file, before filtering. */
  postsFound: number;
  postsImported: number;
  skippedDuplicates: number;
  skippedReposts: number;
  skippedShort: number;
  skippedOverLimit: number;
  postsAnalyzed: number;
  /** Failed analyses; the backend retries these automatically. */
  postsFailed: number;
  postsPending: number;
  earliestPostAt: string | null;
  latestPostAt: string | null;
  createdAt: string;
  completedAt: string | null;
  errorMessage: string | null;
}

export interface SocialMonthPoint {
  month: string;
  label: string;
  posts: number;
  /** -1 to 1. */
  averageSentiment: number;
  /** Share (0–1) of the month's posts reading as stress, anxiety or low mood. */
  concernShare: number;
}

export interface SocialPostInsight {
  id: number;
  analysisId: number;
  provider: SocialProvider;
  postedAt: string | null;
  excerpt: string;
  sentiment: string | null;
  sentimentScore: number | null;
  prediction: string | null;
  predictionConfidence: number | null;
  dominantEmotion: string | null;
}

export interface SocialInsights {
  totalPosts: number;
  analyzedPosts: number;
  pendingPosts: number;
  undatedPosts: number;
  postsByProvider: Record<string, number>;
  predictionDistribution: Record<string, number>;
  averageSentiment: number | null;
  topEmotions: { emotion: string; count: number }[];
  timeline: SocialMonthPoint[];
  recentPosts: SocialPostInsight[];
}

export const MAX_UPLOAD_BYTES = 100 * 1024 * 1024;
export const MAX_POSTS_PER_IMPORT = 1000;
const ACCEPTED_EXTENSIONS = ['.zip', '.json', '.js'];

export interface ProviderGuide {
  id: SocialProvider;
  name: string;
  steps: string[];
  upload: string;
  note: string;
}

/** How to get each platform's export. Menu wording drifts, so steps stay descriptive. */
export const PROVIDER_GUIDES: ProviderGuide[] = [
  {
    id: 'X',
    name: 'X (Twitter)',
    steps: [
      'Open X and go to Settings and privacy → Your account.',
      'Choose "Download an archive of your data" and confirm it is you.',
      'X emails you when the archive is ready, usually within 24 hours.',
    ],
    upload: 'The ZIP X sends you, or data/tweets.js from inside it.',
    note: "Direct messages and reposts of other people's posts are never read.",
  },
  {
    id: 'INSTAGRAM',
    name: 'Instagram',
    steps: [
      'Open Accounts Center → Your information and permissions → Download your information.',
      'Choose your Instagram account and select only "Posts".',
      'Set the format to JSON and media quality to low, then request the download.',
    ],
    upload: 'The ZIP, or your_instagram_activity/media/posts_1.json from inside it.',
    note: 'Only captions are analyzed; posts without a caption are skipped. Messages are never read.',
  },
  {
    id: 'FACEBOOK',
    name: 'Facebook',
    steps: [
      'Open Accounts Center → Your information and permissions → Download your information.',
      'Choose your Facebook account and select only "Posts".',
      'Set the format to JSON, then request the download.',
    ],
    upload: 'The ZIP, or your_posts_1.json from inside it.',
    note: "Messages and other people's comments are never read.",
  },
];

export function providerLabel(provider: SocialProvider): string {
  return PROVIDER_GUIDES.find((guide) => guide.id === provider)?.name ?? provider;
}

/** A reason the file cannot be uploaded, or null when it looks fine. Checked again server-side. */
export function validateExportFile(file: Pick<File, 'name' | 'size'> | null): string | null {
  if (!file) {
    return 'Choose a file to upload.';
  }
  const lower = file.name.toLowerCase();
  if (!ACCEPTED_EXTENSIONS.some((extension) => lower.endsWith(extension))) {
    return 'Upload the .zip export, or the .json / .js posts file from inside it.';
  }
  if (file.size === 0) {
    return 'That file is empty.';
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return 'That file is over 100 MB. Request only your posts, in JSON, without media.';
  }
  return null;
}

/** Percent of an import's posts that have been through analysis, successful or not. */
export function importProgress(item: Pick<SocialImport, 'postsImported' | 'postsAnalyzed' | 'postsFailed'>): number {
  if (item.postsImported === 0) {
    return 100;
  }
  return Math.min(100, Math.round((100 * (item.postsAnalyzed + item.postsFailed)) / item.postsImported));
}

function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
  return count === 1 ? singular : pluralForm;
}

/** Plain-language reasons posts were left out, so a smaller count never looks like data loss. */
export function skippedSummary(
  item: Pick<SocialImport, 'skippedDuplicates' | 'skippedReposts' | 'skippedShort' | 'skippedOverLimit'>,
): string[] {
  const lines: string[] = [];
  if (item.skippedDuplicates > 0) {
    lines.push(`${item.skippedDuplicates} ${plural(item.skippedDuplicates, 'post was', 'posts were')} already imported earlier`);
  }
  if (item.skippedReposts > 0) {
    lines.push(`${item.skippedReposts} ${plural(item.skippedReposts, 'repost')} of other people's posts left out`);
  }
  if (item.skippedShort > 0) {
    lines.push(`${item.skippedShort} ${plural(item.skippedShort, 'post')} with no text, or too short to analyze`);
  }
  if (item.skippedOverLimit > 0) {
    lines.push(
      `${item.skippedOverLimit} older ${plural(item.skippedOverLimit, 'post')} beyond the ${MAX_POSTS_PER_IMPORT.toLocaleString('en-US')}-post limit`,
    );
  }
  return lines;
}

const IMPORTS_KEY = ['social-imports'] as const;

export function useSocialImports() {
  return useQuery({
    queryKey: IMPORTS_KEY,
    queryFn: () => apiRequest<SocialImport[]>('/social-imports'),
    // Analysis runs in the background after upload; follow it while anything is in progress.
    refetchInterval: (query) => (query.state.data?.some((item) => item.status === 'PROCESSING') ? 3000 : false),
  });
}

export function useSocialInsights(followProgress: boolean) {
  return useQuery({
    queryKey: [...IMPORTS_KEY, 'insights'],
    queryFn: () => apiRequest<SocialInsights>('/social-imports/insights'),
    refetchInterval: followProgress ? 4000 : false,
  });
}

export function useUploadSocialExport() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ provider, file, acknowledged }: { provider: SocialProvider; file: File; acknowledged: boolean }) => {
      const form = new FormData();
      form.append('provider', provider);
      form.append('file', file);
      // Sent as given, so the API enforces the acknowledgement rather than trusting the button.
      form.append('acknowledged', String(acknowledged));
      return apiRequest<SocialImport>('/social-imports', { method: 'POST', body: form });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: IMPORTS_KEY }),
  });
}

export function useDeleteSocialImport() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiRequest<void>(`/social-imports/${id}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: IMPORTS_KEY }),
  });
}
