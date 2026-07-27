import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiRequest } from './api';

export interface QuestionnaireResult {
  id: number;
  questionnaireKey: string;
  totalScore: number;
  maxScore: number;
  severity: string;
  submittedAt: string;
}

export interface QuestionnaireSubmitInput {
  questionnaireKey?: string;
  answers: number[];
  optionsPerQuestion?: number;
}

const QUESTIONNAIRE_KEY = ['questionnaire'] as const;

export function useQuestionnaireHistory() {
  return useQuery({
    queryKey: QUESTIONNAIRE_KEY,
    queryFn: () => apiRequest<QuestionnaireResult[]>('/questionnaire/history'),
  });
}

export function useSubmitQuestionnaire() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: QuestionnaireSubmitInput) =>
      apiRequest<QuestionnaireResult>('/questionnaire', { method: 'POST', body: input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUESTIONNAIRE_KEY });
    },
  });
}
