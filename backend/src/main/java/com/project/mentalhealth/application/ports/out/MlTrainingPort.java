package com.project.mentalhealth.application.ports.out;

import java.util.List;
import java.util.Map;

/**
 * Privileged operations against the ML service: training runs and model deployment.
 *
 * <p>Separate from {@link MlAnalysisPort} because these change what every user's predictions
 * come from, and are reachable only by admins.
 */
public interface MlTrainingPort {

    TrainJob startTraining(List<TrainingExample> examples, int maxPerUser);

    TrainJob trainingStatus(String jobId);

    ModelVersions versions();

    PromotionResult promote(String version, boolean force);

    PromotionResult rollback(String version);

    /** One pseudonymized, PII-scrubbed example. Never carries a user identifier. */
    record TrainingExample(String text, String label, String source, double weight, String userHash) {}

    record GateCheck(String name, boolean passed, String detail) {}

    record Gate(boolean passed, List<GateCheck> checks, List<String> failed) {}

    record TrainJob(String jobId,
                    String status,
                    String message,
                    String version,
                    Gate gate,
                    Map<String, Object> corpus,
                    Map<String, Object> metrics) {}

    record ModelVersion(String version,
                        String createdAt,
                        boolean promoted,
                        String promotedAt,
                        Map<String, Object> metrics,
                        Map<String, Object> corpus,
                        Gate gate) {}

    record ModelVersions(String active, List<ModelVersion> versions) {}

    record PromotionResult(String active, boolean reloaded, boolean gatePassed, boolean forced) {}
}
