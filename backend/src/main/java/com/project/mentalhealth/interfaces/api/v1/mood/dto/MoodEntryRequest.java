package com.project.mentalhealth.interfaces.api.v1.mood.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class MoodEntryRequest {
    @Min(value = 0, message = "Mood score must be between 0 and 100")
    @Max(value = 100, message = "Mood score must be between 0 and 100")
    private int moodScore;

    @Size(max = 50, message = "Mood label must be at most 50 characters")
    private String moodLabel;

    private String note;

    private Instant recordedAt;
}
