package com.project.mentalhealth.interfaces.api.v1.goal.dto;

import com.project.mentalhealth.domain.model.WellnessGoal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalResponse {
    private final Long id;
    private final String title;
    private final int target;
    private final int progress;
    private final String period;
    private final boolean completed;

    public static GoalResponse from(WellnessGoal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .target(goal.getTarget())
                .progress(goal.getProgress())
                .period(goal.getPeriod())
                .completed(goal.isCompleted())
                .build();
    }
}
