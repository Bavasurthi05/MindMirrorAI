package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.goal.dto.GoalRequest;
import com.project.mentalhealth.interfaces.api.v1.goal.dto.GoalResponse;

import java.util.List;

public interface GoalUseCase {
    List<GoalResponse> list(String userEmail);
    GoalResponse create(String userEmail, GoalRequest request);
    GoalResponse incrementProgress(String userEmail, Long goalId);
    void delete(String userEmail, Long goalId);
}
