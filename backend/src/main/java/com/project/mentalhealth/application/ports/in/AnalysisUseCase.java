package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.application.ports.out.MlAnalysisPort;

public interface AnalysisUseCase {
    MlAnalysisPort.JournalAnalysis analyzeJournal(String text);
    MlAnalysisPort.JournalAnalysis analyzeSocial(String text);
    MlAnalysisPort.MoodPrediction predictMood(String userEmail);
    MlAnalysisPort.ModelMetrics modelMetrics();
}
