package com.project.mentalhealth.interfaces.api.v1.profile.dto;

import com.project.mentalhealth.domain.model.UserPreferences;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Getter
@Builder
public class UserPreferencesResponse {

    private final String timezone;
    private final boolean reminderEnabled;
    private final String reminderTime;
    private final List<String> focusAreas;
    private final boolean trainingConsent;
    private final Instant trainingConsentAt;
    private final boolean onboardingCompleted;
    private final int onboardingStep;

    public static UserPreferencesResponse from(UserPreferences preferences) {
        String areas = preferences.getFocusAreas();
        return UserPreferencesResponse.builder()
                .timezone(preferences.getTimezone())
                .reminderEnabled(preferences.isReminderEnabled())
                .reminderTime(preferences.getReminderTime())
                .focusAreas(areas == null || areas.isBlank() ? List.of() : Arrays.asList(areas.split(",")))
                .trainingConsent(preferences.isTrainingConsent())
                .trainingConsentAt(preferences.getTrainingConsentAt())
                .onboardingCompleted(preferences.isOnboardingCompleted())
                .onboardingStep(preferences.getOnboardingStep())
                .build();
    }
}
