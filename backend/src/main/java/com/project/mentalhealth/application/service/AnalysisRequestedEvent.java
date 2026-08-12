package com.project.mentalhealth.application.service;

/** Raised when a {@code PENDING} analysis row has been written and needs the ML service. */
public record AnalysisRequestedEvent(Long analysisId) {
}
