package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.AnalysisResultResponse;

import java.util.List;

public interface AnalysisUseCase {

    /** Analyze ad-hoc text and persist the result so it survives a page refresh. */
    AnalysisResultResponse analyzeJournal(String userEmail, String text);

    AnalysisResultResponse analyzeSocial(String userEmail, String text);

    /** Most recent completed analyses, newest first. */
    List<AnalysisResultResponse> recentResults(String userEmail, int limit);

    AnalysisResultResponse result(String userEmail, Long id);

    /** Latest analysis for a journal entry, or null while it is still pending. */
    AnalysisResultResponse resultForJournalEntry(String userEmail, Long journalEntryId);

    MlAnalysisPort.MoodPrediction predictMood(String userEmail);

    MlAnalysisPort.ModelMetrics modelMetrics();

    MlAnalysisPort.MlHealth mlHealth();
}
