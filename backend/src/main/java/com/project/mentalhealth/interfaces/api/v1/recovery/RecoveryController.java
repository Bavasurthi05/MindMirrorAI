package com.project.mentalhealth.interfaces.api.v1.recovery;

import com.project.mentalhealth.application.ports.in.RecoveryUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.recovery.dto.RecoveryActionResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/recovery")
public class RecoveryController {

    private final RecoveryUseCase recoveryUseCase;

    public RecoveryController(RecoveryUseCase recoveryUseCase) {
        this.recoveryUseCase = recoveryUseCase;
    }

    @GetMapping
    public ApiResponse<List<RecoveryActionResponse>> getPlan(Authentication authentication) {
        return ApiResponse.success(recoveryUseCase.getPlan(authentication.getName()));
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<RecoveryActionResponse> toggle(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(recoveryUseCase.toggle(authentication.getName(), id), "Updated");
    }
}
