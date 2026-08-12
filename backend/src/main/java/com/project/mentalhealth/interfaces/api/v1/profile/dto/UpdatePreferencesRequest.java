package com.project.mentalhealth.interfaces.api.v1.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Partial update: every field is optional, and null means "leave unchanged". */
@Getter
@Setter
public class UpdatePreferencesRequest {

    @Size(max = 64)
    private String timezone;

    private Boolean reminderEnabled;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Reminder time must be HH:mm")
    private String reminderTime;

    @Size(max = 8, message = "Choose at most 8 focus areas")
    private List<@Size(max = 40) String> focusAreas;

    private Boolean trainingConsent;

    @Min(0)
    @Max(10)
    private Integer onboardingStep;

    private Boolean onboardingCompleted;
}
