package com.project.mentalhealth.interfaces.api.v1.questionnaire.dto;

import com.project.mentalhealth.domain.model.AssessmentSubmission;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class QuestionnaireResultResponse {
    private final Long id;
    private final String questionnaireKey;
    private final int totalScore;
    private final int maxScore;
    private final String severity;
    private final Instant submittedAt;

    public static QuestionnaireResultResponse from(AssessmentSubmission submission) {
        return QuestionnaireResultResponse.builder()
                .id(submission.getId())
                .questionnaireKey(submission.getQuestionnaireKey())
                .totalScore(submission.getTotalScore())
                .maxScore(submission.getMaxScore())
                .severity(submission.getSeverity())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }
}
