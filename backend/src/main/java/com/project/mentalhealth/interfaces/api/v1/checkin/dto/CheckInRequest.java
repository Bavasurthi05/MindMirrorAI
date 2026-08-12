package com.project.mentalhealth.interfaces.api.v1.checkin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInRequest {

    @Min(value = 0, message = "Mood score must be between 0 and 100")
    @Max(value = 100, message = "Mood score must be between 0 and 100")
    private int moodScore;

    @Size(max = 50, message = "Mood label must be at most 50 characters")
    private String moodLabel;

    /** Optional line of text; analyzed like a journal entry when present. */
    @Size(max = 2000, message = "Note must be at most 2000 characters")
    private String note;
}
