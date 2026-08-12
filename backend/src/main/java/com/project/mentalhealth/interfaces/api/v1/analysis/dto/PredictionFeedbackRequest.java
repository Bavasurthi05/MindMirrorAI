package com.project.mentalhealth.interfaces.api.v1.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PredictionFeedbackRequest {

    /** AGREE, DISAGREE or PARTIAL. */
    @NotBlank(message = "Agreement is required")
    private String agreement;

    /** Required when disagreeing: what the state actually was. */
    @Size(max = 32)
    private String correctedLabel;

    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String comment;
}
