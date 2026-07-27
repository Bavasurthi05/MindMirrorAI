package com.project.mentalhealth.interfaces.api.v1.analysis;

import com.project.mentalhealth.application.ports.in.AnalysisUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.AnalyzeJournalRequest;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/analysis")
public class AnalysisController {

    private final AnalysisUseCase analysisUseCase;

    public AnalysisController(AnalysisUseCase analysisUseCase) {
        this.analysisUseCase = analysisUseCase;
    }

    @PostMapping("/journal")
    public ApiResponse<MlAnalysisPort.JournalAnalysis> analyzeJournal(@Valid @RequestBody AnalyzeJournalRequest request) {
        return ApiResponse.success(analysisUseCase.analyzeJournal(request.getText()));
    }

    @PostMapping("/social")
    public ApiResponse<MlAnalysisPort.JournalAnalysis> analyzeSocial(@Valid @RequestBody AnalyzeJournalRequest request) {
        return ApiResponse.success(analysisUseCase.analyzeSocial(request.getText()));
    }

    @GetMapping("/mood-prediction")
    public ApiResponse<MlAnalysisPort.MoodPrediction> predictMood(Authentication authentication) {
        return ApiResponse.success(analysisUseCase.predictMood(authentication.getName()));
    }

    @GetMapping("/model-metrics")
    public ApiResponse<MlAnalysisPort.ModelMetrics> modelMetrics() {
        return ApiResponse.success(analysisUseCase.modelMetrics());
    }
}
