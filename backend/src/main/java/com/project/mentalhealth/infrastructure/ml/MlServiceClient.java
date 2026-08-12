package com.project.mentalhealth.infrastructure.ml;

import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * HTTP adapter for the ML service.
 *
 * <p>Hardened so a slow or dead ML service degrades the product instead of breaking it:
 * bounded timeouts, one retry on transient failure, and a circuit breaker that stops
 * hammering a service that is already down. Callers that must not fail (journal saves)
 * should invoke this off the request thread.
 */
@Component
public class MlServiceClient implements MlAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_MDC_KEY = "correlationId";

    private final RestClient restClient;
    private final MlCircuitBreaker circuitBreaker;
    private final long retryBackoffMs;

    public MlServiceClient(@Value("${app.ml.base-url}") String baseUrl,
                           @Value("${app.ml.connect-timeout-ms:2000}") long connectTimeoutMs,
                           @Value("${app.ml.read-timeout-ms:8000}") long readTimeoutMs,
                           @Value("${app.ml.retry-backoff-ms:250}") long retryBackoffMs,
                           @Value("${app.ml.circuit-breaker.failure-threshold:5}") int failureThreshold,
                           @Value("${app.ml.circuit-breaker.open-seconds:30}") long openSeconds) {
        this.retryBackoffMs = retryBackoffMs;
        this.circuitBreaker = new MlCircuitBreaker(failureThreshold, Duration.ofSeconds(openSeconds));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                                .withReadTimeout(Duration.ofMillis(readTimeoutMs))))
                .build();
    }

    // --- Public API -------------------------------------------------------------------

    @Override
    public JournalAnalysis analyzeJournal(String text) {
        return callAnalyze("/analyze/journal", text);
    }

    @Override
    public JournalAnalysis analyzeSocial(String text) {
        return callAnalyze("/analyze/social", text);
    }

    @Override
    public List<BatchAnalysisResult> analyzeBatch(List<BatchAnalysisItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        if (items.size() > MAX_BATCH_SIZE) {
            throw new ApiException("Batch size exceeds " + MAX_BATCH_SIZE, HttpStatus.BAD_REQUEST);
        }

        List<Map<String, String>> payload = items.stream()
                .map(item -> Map.of("reference", item.reference(), "text", item.text()))
                .toList();

        BatchAnalysisResponseDto dto = execute("analyzeBatch", () -> restClient.post()
                .uri("/analyze/batch")
                .header(CORRELATION_HEADER, correlationId())
                .body(Map.of("items", payload))
                .retrieve()
                .body(BatchAnalysisResponseDto.class));

        if (dto == null || dto.results() == null) {
            throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
        }
        return dto.results().stream()
                .map(item -> new BatchAnalysisResult(
                        item.reference(),
                        item.analysis() == null ? null : toAnalysis(item.analysis()),
                        item.error()))
                .toList();
    }

    @Override
    public MoodPrediction predictMood(List<Integer> recentScores) {
        MoodPredictionDto dto = execute("predictMood", () -> restClient.post()
                .uri("/predict/mood")
                .header(CORRELATION_HEADER, correlationId())
                .body(Map.of("recent_scores", recentScores))
                .retrieve()
                .body(MoodPredictionDto.class));

        if (dto == null) {
            throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
        }
        return new MoodPrediction(dto.predicted_score(), dto.trend(), dto.confidence(), dto.rationale());
    }

    @Override
    public WeeklyInsights weeklyInsights(List<Integer> moodScores, int journalCount,
                                         int triggerCount, double averageTriggerIntensity) {
        WeeklyInsightsDto dto = execute("weeklyInsights", () -> restClient.post()
                .uri("/insights/weekly")
                .header(CORRELATION_HEADER, correlationId())
                .body(Map.of(
                        "mood_scores", moodScores,
                        "journal_count", journalCount,
                        "trigger_count", triggerCount,
                        "average_trigger_intensity", averageTriggerIntensity))
                .retrieve()
                .body(WeeklyInsightsDto.class));

        if (dto == null) {
            throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
        }
        return new WeeklyInsights(
                dto.highlights() == null ? List.of() : dto.highlights(),
                dto.focus_area(),
                dto.wellbeing_index());
    }

    @Override
    public ModelMetrics modelMetrics() {
        ModelMetricsDto dto = execute("modelMetrics", () -> restClient.get()
                .uri("/models/metrics")
                .header(CORRELATION_HEADER, correlationId())
                .retrieve()
                .body(ModelMetricsDto.class));

        if (dto == null) {
            throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
        }
        Map<String, ModelInfo> models = dto.models() == null ? Map.of()
                : dto.models().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ModelInfo(e.getValue().name(), e.getValue().accuracy(),
                                e.getValue().f1_macro(), e.getValue().deployed())));
        return new ModelMetrics(
                dto.available(),
                dto.backend(),
                dto.version() == null ? "unknown" : dto.version(),
                dto.labels() == null ? List.of() : dto.labels(),
                dto.emotion_labels() == null ? List.of() : dto.emotion_labels(),
                dto.train_size(),
                dto.test_size(),
                dto.dataset_profile() == null ? Map.of() : dto.dataset_profile(),
                models);
    }

    @Override
    public MlHealth health() {
        if (circuitBreaker.isOpen()) {
            return new MlHealth(false, "circuit-open", "unknown", false,
                    "Circuit breaker is open after repeated failures");
        }
        try {
            HealthDto dto = restClient.get()
                    .uri("/health")
                    .header(CORRELATION_HEADER, correlationId())
                    .retrieve()
                    .body(HealthDto.class);
            if (dto == null) {
                return new MlHealth(false, "unknown", "unknown", false, "Empty response");
            }
            return new MlHealth(true, dto.status(),
                    dto.model_version() == null ? "unknown" : dto.model_version(),
                    dto.model_available(), null);
        } catch (RestClientException ex) {
            return new MlHealth(false, "unreachable", "unknown", false, ex.getMessage());
        }
    }

    // --- Internals --------------------------------------------------------------------

    private JournalAnalysis callAnalyze(String uri, String text) {
        JournalAnalysisDto dto = execute("analyze:" + uri, () -> restClient.post()
                .uri(uri)
                .header(CORRELATION_HEADER, correlationId())
                .body(Map.of("text", text))
                .retrieve()
                .body(JournalAnalysisDto.class));

        if (dto == null) {
            throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
        }
        return toAnalysis(dto);
    }

    private JournalAnalysis toAnalysis(JournalAnalysisDto dto) {
        List<TokenContribution> explanation = dto.explanation() == null ? List.of()
                : dto.explanation().stream()
                .map(item -> new TokenContribution(item.token(), item.weight()))
                .toList();
        List<FeatureReason> reasons = dto.reasons() == null ? List.of()
                : dto.reasons().stream()
                .map(item -> new FeatureReason(item.feature(), item.weight(), item.percentage()))
                .toList();
        List<DetectedTrigger> triggers = dto.triggers() == null ? List.of()
                : dto.triggers().stream()
                .map(item -> new DetectedTrigger(
                        item.category(),
                        item.matched_terms() == null ? List.of() : item.matched_terms(),
                        item.intensity()))
                .toList();
        return new JournalAnalysis(
                dto.sentiment(),
                dto.sentiment_score(),
                dto.emotion(),
                dto.emotion_scores() == null ? Map.of() : dto.emotion_scores(),
                explanation,
                dto.prediction(),
                dto.prediction_confidence(),
                dto.prediction_probabilities() == null ? Map.of() : dto.prediction_probabilities(),
                reasons,
                dto.model_backend(),
                triggers,
                dto.model_version() == null ? "unknown" : dto.model_version());
    }

    /**
     * Runs a call under the circuit breaker with a single retry on transient failure.
     *
     * <p>Throws {@link ApiException} with 503 when the ML service is unavailable, so callers
     * can decide whether that is fatal (an explicit "Analyze" request) or tolerable
     * (background analysis, which retries later).
     */
    private <T> T execute(String operation, Supplier<T> call) {
        if (!circuitBreaker.allowRequest()) {
            throw new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }

        try {
            T result = call.get();
            circuitBreaker.recordSuccess();
            return result;
        } catch (RestClientException first) {
            log.warn("ML call {} failed ({}); retrying once", operation, first.getMessage());
            sleepBackoff();
            try {
                T result = call.get();
                circuitBreaker.recordSuccess();
                return result;
            } catch (RestClientException second) {
                circuitBreaker.recordFailure();
                log.error("ML call {} failed after retry: {}", operation, second.getMessage());
                throw new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String correlationId() {
        String existing = MDC.get(CORRELATION_MDC_KEY);
        return existing != null ? existing : UUID.randomUUID().toString();
    }

    // --- Wire DTOs --------------------------------------------------------------------

    private record TokenContributionDto(String token, double weight) {}

    private record FeatureReasonDto(String feature, double weight, double percentage) {}

    private record DetectedTriggerDto(String category, List<String> matched_terms, int intensity) {}

    private record JournalAnalysisDto(String sentiment,
                                      double sentiment_score,
                                      String emotion,
                                      Map<String, Double> emotion_scores,
                                      List<TokenContributionDto> explanation,
                                      String prediction,
                                      double prediction_confidence,
                                      Map<String, Double> prediction_probabilities,
                                      List<FeatureReasonDto> reasons,
                                      String model_backend,
                                      List<DetectedTriggerDto> triggers,
                                      String model_version) {}

    private record BatchAnalysisResultDto(String reference, JournalAnalysisDto analysis, String error) {}

    private record BatchAnalysisResponseDto(List<BatchAnalysisResultDto> results) {}

    private record MoodPredictionDto(double predicted_score, String trend, double confidence, String rationale) {}

    private record WeeklyInsightsDto(List<String> highlights, String focus_area, int wellbeing_index) {}

    private record ModelInfoDto(String name, double accuracy, double f1_macro, boolean deployed) {}

    private record ModelMetricsDto(boolean available,
                                   String backend,
                                   String version,
                                   List<String> labels,
                                   List<String> emotion_labels,
                                   int train_size,
                                   int test_size,
                                   Map<String, Object> dataset_profile,
                                   Map<String, ModelInfoDto> models) {}

    private record HealthDto(String status, String model_version, boolean model_available) {}
}
