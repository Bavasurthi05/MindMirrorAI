package com.project.mentalhealth.application.ports.out;

import java.util.List;
import java.util.Map;

public interface MlAnalysisPort {

    JournalAnalysis analyzeJournal(String text);

    JournalAnalysis analyzeSocial(String text);

    MoodPrediction predictMood(List<Integer> recentScores);

    WeeklyInsights weeklyInsights(List<Integer> moodScores, int journalCount, int triggerCount, double averageTriggerIntensity);

    ModelMetrics modelMetrics();

    record TokenContribution(String token, double weight) {}

    record FeatureReason(String feature, double weight, double percentage) {}

    record JournalAnalysis(String sentiment,
                           double sentimentScore,
                           String emotion,
                           Map<String, Double> emotionScores,
                           List<TokenContribution> explanation,
                           String prediction,
                           double predictionConfidence,
                           Map<String, Double> predictionProbabilities,
                           List<FeatureReason> reasons,
                           String modelBackend) {}

    record MoodPrediction(double predictedScore, String trend, double confidence, String rationale) {}

    record WeeklyInsights(List<String> highlights, String focusArea, int wellbeingIndex) {}

    record ModelInfo(String name, double accuracy, double f1Macro, boolean deployed) {}

    record ModelMetrics(boolean available,
                        String backend,
                        List<String> labels,
                        int trainSize,
                        int testSize,
                        Map<String, ModelInfo> models) {}
}
