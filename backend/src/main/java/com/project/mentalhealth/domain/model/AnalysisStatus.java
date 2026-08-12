package com.project.mentalhealth.domain.model;

/** Lifecycle of an {@link AnalysisResult}. */
public enum AnalysisStatus {
    /** Saved, waiting for the ML service. */
    PENDING,
    /** Analyzed successfully. */
    OK,
    /** Analysis failed after exhausting retries. */
    FAILED
}
