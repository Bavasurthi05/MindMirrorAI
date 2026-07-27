package com.project.mentalhealth.interfaces.api.v1.mood.dto;

import com.project.mentalhealth.domain.model.MoodEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MoodEntryResponse {
    private final Long id;
    private final int moodScore;
    private final String moodLabel;
    private final String note;
    private final Instant recordedAt;

    public static MoodEntryResponse from(MoodEntry entry) {
        return MoodEntryResponse.builder()
                .id(entry.getId())
                .moodScore(entry.getMoodScore())
                .moodLabel(entry.getMoodLabel())
                .note(entry.getNote())
                .recordedAt(entry.getRecordedAt())
                .build();
    }
}
