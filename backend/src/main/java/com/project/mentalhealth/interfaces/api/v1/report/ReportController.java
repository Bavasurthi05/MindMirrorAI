package com.project.mentalhealth.interfaces.api.v1.report;

import com.project.mentalhealth.application.ports.in.ReportUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.report.dto.ReportSummaryResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/reports")
public class ReportController {

    private final ReportUseCase reportUseCase;

    public ReportController(ReportUseCase reportUseCase) {
        this.reportUseCase = reportUseCase;
    }

    @GetMapping("/summary")
    public ApiResponse<ReportSummaryResponse> summary(Authentication authentication) {
        return ApiResponse.success(reportUseCase.summary(authentication.getName()));
    }
}
