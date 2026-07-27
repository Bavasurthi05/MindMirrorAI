package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminUserResponse;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackResponse;

import java.util.List;

public interface AdminUseCase {
    AdminOverviewResponse overview();
    List<AdminUserResponse> listUsers();
    AdminUserResponse setUserEnabled(Long userId, boolean enabled);
    List<FeedbackResponse> listFeedback();
    MlAnalysisPort.ModelMetrics modelMetrics();
}
