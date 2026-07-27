package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.recovery.dto.RecoveryActionResponse;

import java.util.List;

public interface RecoveryUseCase {
    List<RecoveryActionResponse> getPlan(String userEmail);
    RecoveryActionResponse toggle(String userEmail, Long actionId);
}
