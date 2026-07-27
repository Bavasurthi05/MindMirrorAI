package com.project.mentalhealth.interfaces.api.v1.questionnaire.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionnaireSubmitRequest {
    private String questionnaireKey = "wellbeing";

    @NotEmpty(message = "At least one answer is required")
    private List<@NotNull Integer> answers;

    private int optionsPerQuestion = 4;
}
