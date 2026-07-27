package com.project.mentalhealth.interfaces.api.v1.trigger.dto;

import com.project.mentalhealth.domain.model.TriggerEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TriggerEntryResponse {
    private final Long id;
    private final String category;
    private final int intensity;
    private final String note;
    private final Instant occurredAt;

    public static TriggerEntryResponse from(TriggerEntry entry) {
        return TriggerEntryResponse.builder()
                .id(entry.getId())
                .category(entry.getCategory())
                .intensity(entry.getIntensity())
                .note(entry.getNote())
                .occurredAt(entry.getOccurredAt())
                .build();
    }
}
