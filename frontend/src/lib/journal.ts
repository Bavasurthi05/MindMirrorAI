import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface JournalEntry {
  id: number;
  title: string;
  content: string;
  mood: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface JournalPage {
  content: JournalEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface JournalEntryInput {
  title: string;
  content: string;
  mood?: string;
}

const JOURNAL_KEY = ['journal'] as const;

export function useJournalEntries(page = 0, size = 10) {
  return useQuery({
    queryKey: [...JOURNAL_KEY, page, size],
    queryFn: () => apiRequest<JournalPage>(`/journal?page=${page}&size=${size}`),
  });
}

export function useCreateJournalEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: JournalEntryInput) =>
      apiRequest<JournalEntry>('/journal', { method: 'POST', body: input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: JOURNAL_KEY });
    },
  });
}
