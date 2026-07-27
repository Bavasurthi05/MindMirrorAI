package com.project.mentalhealth.interfaces.api.v1.recovery.dto;

import com.project.mentalhealth.domain.model.RecoveryAction;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecoveryActionResponse {
    private final Long id;
    private final String title;
    private final String focus;
    private final String duration;
    private final String description;
    private final boolean completed;

    public static RecoveryActionResponse from(RecoveryAction action) {
        return RecoveryActionResponse.builder()
                .id(action.getId())
                .title(action.getTitle())
                .focus(action.getFocus())
                .duration(action.getDuration())
                .description(action.getDescription())
                .completed(action.isCompleted())
                .build();
    }
}
