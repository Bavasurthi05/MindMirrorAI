package com.project.mentalhealth.interfaces.api.v1.analytics;

import com.project.mentalhealth.application.ports.in.AnalyticsUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.AnalyticsOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/analytics")
public class AnalyticsController {

    private final AnalyticsUseCase analyticsUseCase;

    public AnalyticsController(AnalyticsUseCase analyticsUseCase) {
        this.analyticsUseCase = analyticsUseCase;
    }

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverviewResponse> overview(Authentication authentication) {
        return ApiResponse.success(analyticsUseCase.overview(authentication.getName()));
    }

    @GetMapping("/weekly-insights")
    public ApiResponse<MlAnalysisPort.WeeklyInsights> weeklyInsights(Authentication authentication) {
        return ApiResponse.success(analyticsUseCase.weeklyInsights(authentication.getName()));
    }
}
