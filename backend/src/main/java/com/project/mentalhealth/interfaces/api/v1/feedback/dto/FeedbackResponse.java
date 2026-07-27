package com.project.mentalhealth.interfaces.api.v1.feedback.dto;

import com.project.mentalhealth.domain.model.Feedback;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FeedbackResponse {
    private final Long id;
    private final int rating;
    private final String message;
    private final String userName;
    private final String userEmail;
    private final Instant createdAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .rating(feedback.getRating())
                .message(feedback.getMessage())
                .userName(feedback.getUser().getFirstName() + " " + feedback.getUser().getLastName())
                .userEmail(feedback.getUser().getEmail())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
