package com.project.mentalhealth.interfaces.api.v1.journal.dto;

import com.project.mentalhealth.domain.model.JournalEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class JournalEntryResponse {
    private final Long id;
    private final String title;
    private final String content;
    private final String mood;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static JournalEntryResponse from(JournalEntry entry) {
        return JournalEntryResponse.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .content(entry.getContent())
                .mood(entry.getMood())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
