package com.project.mentalhealth.interfaces.api.v1.analysis.dto;

import com.project.mentalhealth.domain.model.PredictionFeedback;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PredictionFeedbackResponse {

    private final Long id;
    private final Long analysisResultId;
    private final String predictedLabel;
    private final String correctedLabel;
    private final String agreement;
    private final String comment;
    private final Instant createdAt;

    public static PredictionFeedbackResponse from(PredictionFeedback feedback) {
        return PredictionFeedbackResponse.builder()
                .id(feedback.getId())
                .analysisResultId(feedback.getAnalysisResult().getId())
                .predictedLabel(feedback.getPredictedLabel())
                .correctedLabel(feedback.getCorrectedLabel())
                .agreement(feedback.getAgreement().name())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
