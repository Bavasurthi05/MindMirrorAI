package com.project.mentalhealth.infrastructure.ml;

import com.project.mentalhealth.application.ports.out.MlTrainingPort;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP adapter for the ML service's training and registry endpoints.
 *
 * <p>Separate client from {@link MlServiceClient}: training runs take minutes rather than
 * milliseconds, so the timeouts are wholly different, and these calls carry the shared
 * secret that guards model deployment.
 */
@Component
public class MlTrainingClient implements MlTrainingPort {

    private static final String TOKEN_HEADER = "X-Training-Token";

    private final RestClient restClient;
    private final String trainingToken;

    public MlTrainingClient(@Value("${app.ml.base-url}") String baseUrl,
                            @Value("${app.ml.training.token:}") String trainingToken,
                            @Value("${app.ml.training.request-timeout-ms:120000}") long requestTimeoutMs) {
        this.trainingToken = trainingToken;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                                .withConnectTimeout(Duration.ofSeconds(5))
                                // Starting a run returns immediately, but corpus assembly on a
                                // large export can still take a while.
                                .withReadTimeout(Duration.ofMillis(requestTimeoutMs))))
                .build();
    }

    @Override
    public TrainJob startTraining(List<TrainingExample> examples, int maxPerUser) {
        List<Map<String, Object>> payload = examples.stream()
                .map(example -> Map.<String, Object>of(
                        "text", example.text(),
                        "label", example.label(),
                        "source", example.source(),
                        "weight", example.weight(),
                        "user_hash", example.userHash()))
                .toList();

        TrainJobDto dto = call(() -> restClient.post()
                .uri("/train/run")
                .header(TOKEN_HEADER, trainingToken)
                .body(Map.of("examples", payload, "max_per_user", maxPerUser))
                .retrieve()
                .body(TrainJobDto.class));
        return toJob(dto);
    }

    @Override
    public TrainJob trainingStatus(String jobId) {
        TrainJobDto dto = call(() -> restClient.get()
                .uri("/train/status/{jobId}", jobId)
                .header(TOKEN_HEADER, trainingToken)
                .retrieve()
                .body(TrainJobDto.class));
        return toJob(dto);
    }

    @Override
    public ModelVersions versions() {
        ModelVersionsDto dto = call(() -> restClient.get()
                .uri("/models/versions")
                .retrieve()
                .body(ModelVersionsDto.class));
        if (dto == null) {
            return new ModelVersions(null, List.of());
        }
        return new ModelVersions(
                dto.active(),
                dto.versions() == null ? List.of() : dto.versions().stream().map(this::toVersion).toList());
    }

    @Override
    public PromotionResult promote(String version, boolean force) {
        PromotionDto dto = call(() -> restClient.post()
                .uri("/models/promote")
                .header(TOKEN_HEADER, trainingToken)
                .body(Map.of("version", version, "force", force))
                .retrieve()
                .body(PromotionDto.class));
        return toPromotion(dto);
    }

    @Override
    public PromotionResult rollback(String version) {
        PromotionDto dto = call(() -> restClient.post()
                .uri("/models/rollback")
                .header(TOKEN_HEADER, trainingToken)
                .body(Map.of("version", version, "force", false))
                .retrieve()
                .body(PromotionDto.class));
        return toPromotion(dto);
    }

    /**
     * Surfaces the ML service's own refusal instead of flattening it into a generic 503 —
     * "this version failed the gate" is information the admin needs to act on.
     */
    private <T> T call(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (HttpClientErrorException ex) {
            throw new ApiException(extractDetail(ex), HttpStatus.valueOf(ex.getStatusCode().value()));
        } catch (RestClientException ex) {
            throw new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String extractDetail(HttpClientErrorException ex) {
        try {
            Map<?, ?> body = ex.getResponseBodyAs(Map.class);
            Object detail = body == null ? null : body.get("detail");
            if (detail != null) {
                return String.valueOf(detail);
            }
        } catch (Exception ignored) {
            // fall through to the generic message
        }
        return "ML service rejected the request";
    }

    private TrainJob toJob(TrainJobDto dto) {
        if (dto == null) {
            throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
        }
        return new TrainJob(dto.job_id(), dto.status(), dto.message(), dto.version(),
                toGate(dto.gate()), dto.corpus(), dto.metrics());
    }

    private ModelVersion toVersion(ModelVersionDto dto) {
        return new ModelVersion(dto.version(), dto.created_at(), dto.promoted(), dto.promoted_at(),
                dto.metrics() == null ? Map.of() : dto.metrics(),
                dto.corpus() == null ? Map.of() : dto.corpus(),
                toGate(dto.gate()));
    }

    @SuppressWarnings("unchecked")
    private Gate toGate(Map<String, Object> raw) {
        if (raw == null) {
            return new Gate(false, List.of(), List.of());
        }
        List<GateCheck> checks = List.of();
        Object rawChecks = raw.get("checks");
        if (rawChecks instanceof List<?> list) {
            checks = list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .map(item -> new GateCheck(
                            String.valueOf(item.get("name")),
                            Boolean.TRUE.equals(item.get("passed")),
                            String.valueOf(item.get("detail"))))
                    .toList();
        }
        List<String> failed = raw.get("failed") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return new Gate(Boolean.TRUE.equals(raw.get("passed")), checks, failed);
    }

    private PromotionResult toPromotion(PromotionDto dto) {
        if (dto == null) {
            throw new ApiException("Empty response from ML service", HttpStatus.BAD_GATEWAY);
        }
        return new PromotionResult(dto.active(), dto.reloaded(), dto.gate_passed(), dto.forced());
    }

    private record TrainJobDto(String job_id, String status, String message, String started_at,
                               String finished_at, String version, Map<String, Object> gate,
                               Map<String, Object> corpus, Map<String, Object> metrics) {}

    private record ModelVersionDto(String version, String created_at, boolean promoted, String promoted_at,
                                   Map<String, Object> metrics, Map<String, Object> corpus,
                                   Map<String, Object> gate) {}

    private record ModelVersionsDto(String active, List<ModelVersionDto> versions) {}

    private record PromotionDto(String active, boolean reloaded, boolean gate_passed, boolean forced) {}
}
