package com.project.mentalhealth.interfaces.api.v1.analysis;

import com.project.mentalhealth.application.ports.in.AnalysisUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.application.service.PredictionFeedbackService;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.AnalysisResultResponse;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.AnalyzeJournalRequest;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.PredictionFeedbackRequest;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.PredictionFeedbackResponse;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/analysis")
public class AnalysisController {

    private final AnalysisUseCase analysisUseCase;
    private final PredictionFeedbackService feedbackService;

    public AnalysisController(AnalysisUseCase analysisUseCase, PredictionFeedbackService feedbackService) {
        this.analysisUseCase = analysisUseCase;
        this.feedbackService = feedbackService;
    }

    /** "Was this right?" — the highest-quality training label the app can collect. */
    @PostMapping("/results/{id}/feedback")
    public ApiResponse<PredictionFeedbackResponse> submitFeedback(Authentication authentication,
                                                                  @PathVariable Long id,
                                                                  @Valid @RequestBody PredictionFeedbackRequest request) {
        return ApiResponse.success(feedbackService.submit(authentication.getName(), id, request),
                "Thanks — this helps improve your insights");
    }

    /** The user's existing verdict on an analysis, or null data when they have not answered. */
    @GetMapping("/results/{id}/feedback")
    public ApiResponse<PredictionFeedbackResponse> feedback(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(feedbackService.forAnalysis(authentication.getName(), id));
    }

    @PostMapping("/journal")
    public ApiResponse<AnalysisResultResponse> analyzeJournal(Authentication authentication,
                                                              @Valid @RequestBody AnalyzeJournalRequest request) {
        return ApiResponse.success(analysisUseCase.analyzeJournal(authentication.getName(), request.getText()));
    }

    @PostMapping("/social")
    public ApiResponse<AnalysisResultResponse> analyzeSocial(Authentication authentication,
                                                             @Valid @RequestBody AnalyzeJournalRequest request) {
        return ApiResponse.success(analysisUseCase.analyzeSocial(authentication.getName(), request.getText()));
    }

    @GetMapping("/results")
    public ApiResponse<List<AnalysisResultResponse>> results(Authentication authentication,
                                                             @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(analysisUseCase.recentResults(authentication.getName(), limit));
    }

    @GetMapping("/results/{id}")
    public ApiResponse<AnalysisResultResponse> result(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(analysisUseCase.result(authentication.getName(), id));
    }

    /** Returns null data while the entry's analysis is still pending. */
    @GetMapping("/journal/{journalEntryId}")
    public ApiResponse<AnalysisResultResponse> resultForEntry(Authentication authentication,
                                                              @PathVariable Long journalEntryId) {
        return ApiResponse.success(analysisUseCase.resultForJournalEntry(authentication.getName(), journalEntryId));
    }

    @GetMapping("/mood-prediction")
    public ApiResponse<MlAnalysisPort.MoodPrediction> predictMood(Authentication authentication) {
        return ApiResponse.success(analysisUseCase.predictMood(authentication.getName()));
    }

    @GetMapping("/model-metrics")
    public ApiResponse<MlAnalysisPort.ModelMetrics> modelMetrics() {
        return ApiResponse.success(analysisUseCase.modelMetrics());
    }

    @GetMapping("/health")
    public ApiResponse<MlAnalysisPort.MlHealth> mlHealth() {
        return ApiResponse.success(analysisUseCase.mlHealth());
    }
}
