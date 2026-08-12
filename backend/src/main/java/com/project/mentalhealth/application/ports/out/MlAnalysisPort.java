package com.project.mentalhealth.application.ports.out;

import java.util.List;
import java.util.Map;

public interface MlAnalysisPort {

    JournalAnalysis analyzeJournal(String text);

    JournalAnalysis analyzeSocial(String text);

    /**
     * Analyze several texts in one round trip.
     *
     * @param items caller-referenced texts, at most {@link #MAX_BATCH_SIZE} per call
     * @return one result per item, in request order; a failed item carries an {@code error}
     */
    List<BatchAnalysisResult> analyzeBatch(List<BatchAnalysisItem> items);

    MoodPrediction predictMood(List<Integer> recentScores);

    WeeklyInsights weeklyInsights(List<Integer> moodScores, int journalCount, int triggerCount, double averageTriggerIntensity);

    ModelMetrics modelMetrics();

    /** Reachability and active model version, for the admin panel. */
    MlHealth health();

    int MAX_BATCH_SIZE = 50;

    record TokenContribution(String token, double weight) {}

    record FeatureReason(String feature, double weight, double percentage) {}

    record DetectedTrigger(String category, List<String> matchedTerms, int intensity) {}

    record JournalAnalysis(String sentiment,
                           double sentimentScore,
                           String emotion,
                           Map<String, Double> emotionScores,
                           List<TokenContribution> explanation,
                           String prediction,
                           double predictionConfidence,
                           Map<String, Double> predictionProbabilities,
                           List<FeatureReason> reasons,
                           String modelBackend,
                           List<DetectedTrigger> triggers,
                           String modelVersion) {}

    record BatchAnalysisItem(String reference, String text) {}

    record BatchAnalysisResult(String reference, JournalAnalysis analysis, String error) {}

    record MoodPrediction(double predictedScore, String trend, double confidence, String rationale) {}

    record WeeklyInsights(List<String> highlights, String focusArea, int wellbeingIndex) {}

    record ModelInfo(String name, double accuracy, double f1Macro, boolean deployed) {}

    record ModelMetrics(boolean available,
                        String backend,
                        String version,
                        List<String> labels,
                        List<String> emotionLabels,
                        int trainSize,
                        int testSize,
                        Map<String, Object> datasetProfile,
                        Map<String, ModelInfo> models) {}

    record MlHealth(boolean reachable, String status, String modelVersion, boolean modelAvailable, String detail) {}
}
