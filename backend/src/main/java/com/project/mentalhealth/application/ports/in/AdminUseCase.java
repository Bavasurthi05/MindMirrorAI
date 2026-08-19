package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminAuditEntryResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminUserResponse;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackResponse;

import java.util.List;

public interface AdminUseCase {
    AdminOverviewResponse overview();
    List<AdminUserResponse> listUsers();
    /** @param actorEmail the admin performing the change, recorded in the audit log */
    AdminUserResponse setUserEnabled(String actorEmail, Long userId, boolean enabled);

    /**
     * Assign a role. Refuses changes that would lock everyone out — an admin changing
     * their own role, or demoting the last active administrator.
     */
    AdminUserResponse setUserRole(String actorEmail, Long userId, String roleName);

    List<AdminAuditEntryResponse> auditLog(int limit);
    List<FeedbackResponse> listFeedback();
    MlAnalysisPort.ModelMetrics modelMetrics();
}
