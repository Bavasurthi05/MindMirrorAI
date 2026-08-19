package com.project.mentalhealth.interfaces.api.v1.admin;

import com.project.mentalhealth.application.ports.in.AdminUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.application.ports.out.MlTrainingPort;
import com.project.mentalhealth.application.service.ModelTrainingService;
import com.project.mentalhealth.application.service.PredictionFeedbackService;
import com.project.mentalhealth.application.service.TrainingDataExportService;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminAuditEntryResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminUserResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.FeedbackStatsResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.TrainingRunResponse;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.PredictionFeedbackResponse;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final PredictionFeedbackService predictionFeedbackService;
    private final ModelTrainingService modelTrainingService;
    private final TrainingDataExportService trainingDataExportService;

    public AdminController(AdminUseCase adminUseCase,
                           PredictionFeedbackService predictionFeedbackService,
                           ModelTrainingService modelTrainingService,
                           TrainingDataExportService trainingDataExportService) {
        this.adminUseCase = adminUseCase;
        this.predictionFeedbackService = predictionFeedbackService;
        this.modelTrainingService = modelTrainingService;
        this.trainingDataExportService = trainingDataExportService;
    }

    // --- Retraining -------------------------------------------------------------------

    /** How many consented, scrubbed examples a retrain would actually have to learn from. */
    @GetMapping("/training-data/summary")
    public ApiResponse<TrainingDataExportService.ExportSummary> trainingDataSummary() {
        return ApiResponse.success(trainingDataExportService.summary());
    }

    @PostMapping("/models/retrain")
    public ApiResponse<TrainingRunResponse> retrain(Authentication authentication) {
        return ApiResponse.success(modelTrainingService.startRun(authentication.getName()),
                "Training run started");
    }

    @GetMapping("/models/runs/{jobId}")
    public ApiResponse<TrainingRunResponse> runStatus(@PathVariable String jobId) {
        return ApiResponse.success(modelTrainingService.refreshRun(jobId));
    }

    @GetMapping("/models/runs")
    public ApiResponse<List<TrainingRunResponse>> runs(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(modelTrainingService.history(limit));
    }

    @GetMapping("/models/versions")
    public ApiResponse<MlTrainingPort.ModelVersions> modelVersions() {
        return ApiResponse.success(modelTrainingService.versions());
    }

    /** Deploys a version. A gate failure is refused unless the admin explicitly forces it. */
    @PostMapping("/models/promote")
    public ApiResponse<MlTrainingPort.PromotionResult> promote(Authentication authentication,
                                                               @RequestParam String version,
                                                               @RequestParam(defaultValue = "false") boolean force) {
        return ApiResponse.success(
                modelTrainingService.promote(version, force, authentication.getName()),
                "Model " + version + " is now live");
    }

    @PostMapping("/models/rollback")
    public ApiResponse<MlTrainingPort.PromotionResult> rollback(Authentication authentication,
                                                                @RequestParam String version) {
        return ApiResponse.success(modelTrainingService.rollback(version, authentication.getName()),
                "Rolled back to " + version);
    }

    /** Whether enough corrections have accumulated, and across enough classes, to retrain on. */
    @GetMapping("/prediction-feedback/stats")
    public ApiResponse<FeedbackStatsResponse> predictionFeedbackStats() {
        return ApiResponse.success(predictionFeedbackService.stats());
    }

    @GetMapping("/prediction-feedback")
    public ApiResponse<List<PredictionFeedbackResponse>> predictionFeedback(
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(predictionFeedbackService.recent(limit));
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
    public ApiResponse<AdminUserResponse> setEnabled(Authentication authentication,
                                                     @PathVariable Long id,
                                                     @RequestParam boolean enabled) {
        return ApiResponse.success(adminUseCase.setUserEnabled(authentication.getName(), id, enabled),
                enabled ? "Account enabled" : "Account disabled");
    }

    /** Promote or demote a user. Guarded against self-changes and last-admin demotion. */
    @PatchMapping("/users/{id}/role")
    public ApiResponse<AdminUserResponse> setRole(Authentication authentication,
                                                  @PathVariable Long id,
                                                  @RequestParam String role) {
        return ApiResponse.success(adminUseCase.setUserRole(authentication.getName(), id, role),
                "Role updated");
    }

    /** Recent privileged account changes. */
    @GetMapping("/audit-log")
    public ApiResponse<List<AdminAuditEntryResponse>> auditLog(@RequestParam(defaultValue = "25") int limit) {
        return ApiResponse.success(adminUseCase.auditLog(limit));
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
