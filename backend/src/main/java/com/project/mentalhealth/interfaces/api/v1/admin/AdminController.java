package com.project.mentalhealth.interfaces.api.v1.admin;

import com.project.mentalhealth.application.ports.in.AdminUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminUserResponse;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminUseCase adminUseCase;

    public AdminController(AdminUseCase adminUseCase) {
        this.adminUseCase = adminUseCase;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> overview() {
        return ApiResponse.success(adminUseCase.overview());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> users() {
        return ApiResponse.success(adminUseCase.listUsers());
    }

    @PatchMapping("/users/{id}/enabled")
    public ApiResponse<AdminUserResponse> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return ApiResponse.success(adminUseCase.setUserEnabled(id, enabled));
    }

    @GetMapping("/feedback")
    public ApiResponse<List<FeedbackResponse>> feedback() {
        return ApiResponse.success(adminUseCase.listFeedback());
    }

    @GetMapping("/model-metrics")
    public ApiResponse<MlAnalysisPort.ModelMetrics> modelMetrics() {
        return ApiResponse.success(adminUseCase.modelMetrics());
    }
}
