package com.project.mentalhealth.infrastructure.ml;

import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class MlServiceClient implements MlAnalysisPort {

    private final RestClient restClient;

    public MlServiceClient(@Value("${app.ml.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public JournalAnalysis analyzeJournal(String text) {
        return callAnalyze("/analyze/journal", text);
    }

    @Override
    public JournalAnalysis analyzeSocial(String text) {
        return callAnalyze("/analyze/social", text);
    }

    private JournalAnalysis callAnalyze(String uri, String text) {
        try {
            JournalAnalysisDto dto = restClient.post()
                    .uri(uri)
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(JournalAnalysisDto.class);
            if (dto == null) {
                throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
            }
            List<TokenContribution> explanation = dto.explanation() == null ? List.of()
                    : dto.explanation().stream()
                    .map(item -> new TokenContribution(item.token(), item.weight()))
                    .toList();
            List<FeatureReason> reasons = dto.reasons() == null ? List.of()
                    : dto.reasons().stream()
                    .map(item -> new FeatureReason(item.feature(), item.weight(), item.percentage()))
                    .toList();
            return new JournalAnalysis(
                    dto.sentiment(),
                    dto.sentiment_score(),
                    dto.emotion(),
                    dto.emotion_scores(),
                    explanation,
                    dto.prediction(),
                    dto.prediction_confidence(),
                    dto.prediction_probabilities() == null ? Map.of() : dto.prediction_probabilities(),
                    reasons,
                    dto.model_backend());
        } catch (RestClientException ex) {
            throw new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public MoodPrediction predictMood(List<Integer> recentScores) {
        try {
            MoodPredictionDto dto = restClient.post()
                    .uri("/predict/mood")
                    .body(Map.of("recent_scores", recentScores))
                    .retrieve()
                    .body(MoodPredictionDto.class);
            if (dto == null) {
                throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
            }
            return new MoodPrediction(dto.predicted_score(), dto.trend(), dto.confidence(), dto.rationale());
        } catch (RestClientException ex) {
            throw new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public WeeklyInsights weeklyInsights(List<Integer> moodScores, int journalCount,
                                         int triggerCount, double averageTriggerIntensity) {
        try {
            WeeklyInsightsDto dto = restClient.post()
                    .uri("/insights/weekly")
                    .body(Map.of(
                            "mood_scores", moodScores,
                            "journal_count", journalCount,
                            "trigger_count", triggerCount,
                            "average_trigger_intensity", averageTriggerIntensity))
                    .retrieve()
                    .body(WeeklyInsightsDto.class);
            if (dto == null) {
                throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
            }
            return new WeeklyInsights(
                    dto.highlights() == null ? List.of() : dto.highlights(),
                    dto.focus_area(),
                    dto.wellbeing_index());
        } catch (RestClientException ex) {
            throw new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public ModelMetrics modelMetrics() {
        try {
            ModelMetricsDto dto = restClient.get()
                    .uri("/models/metrics")
                    .retrieve()
                    .body(ModelMetricsDto.class);
            if (dto == null) {
                throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
            }
            Map<String, ModelInfo> models = dto.models() == null ? Map.of()
                    : dto.models().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new ModelInfo(e.getValue().name(), e.getValue().accuracy(),
                                    e.getValue().f1_macro(), e.getValue().deployed())));
            return new ModelMetrics(dto.available(), dto.backend(),
                    dto.labels() == null ? List.of() : dto.labels(),
                    dto.train_size(), dto.test_size(), models);
        } catch (RestClientException ex) {
            throw new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private record TokenContributionDto(String token, double weight) {}

    private record FeatureReasonDto(String feature, double weight, double percentage) {}

    private record JournalAnalysisDto(String sentiment,
                                      double sentiment_score,
                                      String emotion,
                                      Map<String, Double> emotion_scores,
                                      List<TokenContributionDto> explanation,
                                      String prediction,
                                      double prediction_confidence,
                                      Map<String, Double> prediction_probabilities,
                                      List<FeatureReasonDto> reasons,
                                      String model_backend) {}

    private record MoodPredictionDto(double predicted_score, String trend, double confidence, String rationale) {}

    private record WeeklyInsightsDto(List<String> highlights, String focus_area, int wellbeing_index) {}

    private record ModelInfoDto(String name, double accuracy, double f1_macro, boolean deployed) {}

    private record ModelMetricsDto(boolean available,
                                   String backend,
                                   List<String> labels,
                                   int train_size,
                                   int test_size,
                                   Map<String, ModelInfoDto> models) {}
}
