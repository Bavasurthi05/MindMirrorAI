package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.report.dto.ReportSummaryResponse;

public interface ReportUseCase {
    ReportSummaryResponse summary(String userEmail);
}
