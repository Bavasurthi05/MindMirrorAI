package com.project.mentalhealth.interfaces.api.v1.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeJournalRequest {
    @NotBlank(message = "Text is required")
    private String text;
}
