package com.project.mentalhealth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mentalhealth.application.ports.out.MlTrainingPort;
import com.project.mentalhealth.domain.model.ModelTrainingRun;
import com.project.mentalhealth.domain.repository.ModelTrainingRunRepository;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.TrainingRunResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates retraining: export -> train -> gate -> (human) promotion, with an audit row
 * for every run.
 *
 * <p>Promotion is deliberately never automatic. A run that clears the gate is a candidate,
 * not a deployment — a person decides when the model users depend on changes.
 */
@Service
public class ModelTrainingService {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainingService.class);

    private final MlTrainingPort trainingPort;
    private final TrainingDataExportService exportService;
    private final ModelTrainingRunRepository runRepository;
    private final ObjectMapper objectMapper;
    private final int maxPerUser;
    private final boolean scheduledEnabled;

    public ModelTrainingService(MlTrainingPort trainingPort,
                                TrainingDataExportService exportService,
                                ModelTrainingRunRepository runRepository,
                                ObjectMapper objectMapper,
                                @Value("${app.ml.training.max-per-user:200}") int maxPerUser,
                                @Value("${app.ml.training.scheduled-enabled:false}") boolean scheduledEnabled) {
        this.trainingPort = trainingPort;
        this.exportService = exportService;
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
        this.maxPerUser = maxPerUser;
        this.scheduledEnabled = scheduledEnabled;
    }

    @Transactional
    public TrainingRunResponse startRun(String triggeredBy) {
        List<MlTrainingPort.TrainingExample> examples = exportService.export();
        if (examples.isEmpty()) {
            throw new ApiException(
                    "No consented training examples are available yet. Users must opt in and provide "
                            + "prediction feedback before a retrain can learn anything.",
                    HttpStatus.CONFLICT);
        }

        MlTrainingPort.TrainJob job = trainingPort.startTraining(examples, maxPerUser);

        ModelTrainingRun run = new ModelTrainingRun();
        run.setJobId(job.jobId());
        run.setStatus(job.status());
        run.setTriggeredBy(triggeredBy);
        run.setCorpusUserExamples(examples.size());
        run.setMessage(job.message());
        return TrainingRunResponse.from(runRepository.save(run), job);
    }

    /** Polls the ML service and folds the outcome into the audit row. */
    @Transactional
    public TrainingRunResponse refreshRun(String jobId) {
        MlTrainingPort.TrainJob job = trainingPort.trainingStatus(jobId);
        ModelTrainingRun run = runRepository.findFirstByJobIdOrderByIdDesc(jobId)
                .orElseThrow(() -> new ApiException("Training run not found", HttpStatus.NOT_FOUND));

        run.setStatus(job.status());
        run.setMessage(job.message());
        run.setVersion(job.version());

        if (job.corpus() != null) {
            run.setCorpusTotal(intValue(job.corpus().get("total_examples")));
            run.setCorpusUserExamples(intValue(job.corpus().get("user_examples")));
        }
        if (job.metrics() != null) {
            Map<String, Object> models = asMap(job.metrics().get("models"));
            Map<String, Object> forest = asMap(models.get("random_forest"));
            run.setAccuracy(doubleValue(forest.get("accuracy")));
            run.setF1Macro(doubleValue(forest.get("f1_macro")));
        }
        if (job.gate() != null) {
            run.setGatePassed(job.gate().passed());
            run.setGateDetail(writeJson(job.gate()));
        }

        return TrainingRunResponse.from(runRepository.save(run), job);
    }

    @Transactional
    public MlTrainingPort.PromotionResult promote(String version, boolean force, String promotedBy) {
        MlTrainingPort.PromotionResult result = trainingPort.promote(version, force);

        runRepository.findFirstByVersionOrderByIdDesc(version).ifPresent(run -> {
            run.setPromoted(true);
            run.setPromotedAt(Instant.now());
            run.setPromotedBy(promotedBy);
            run.setForced(result.forced());
            runRepository.save(run);
        });

        if (result.forced()) {
            log.warn("{} force-promoted {} despite a failed quality gate", promotedBy, version);
        } else {
            log.info("{} promoted model {}", promotedBy, version);
        }
        return result;
    }

    @Transactional
    public MlTrainingPort.PromotionResult rollback(String version, String triggeredBy) {
        MlTrainingPort.PromotionResult result = trainingPort.rollback(version);
        log.warn("{} rolled the deployed model back to {}", triggeredBy, version);

        ModelTrainingRun run = new ModelTrainingRun();
        run.setVersion(version);
        run.setStatus("ROLLBACK");
        run.setTriggeredBy(triggeredBy);
        run.setPromoted(true);
        run.setPromotedAt(Instant.now());
        run.setPromotedBy(triggeredBy);
        run.setGatePassed(result.gatePassed());
        run.setMessage("Rolled back to " + version);
        runRepository.save(run);

        return result;
    }

    @Transactional(readOnly = true)
    public MlTrainingPort.ModelVersions versions() {
        return trainingPort.versions();
    }

    @Transactional(readOnly = true)
    public List<TrainingRunResponse> history(int limit) {
        return runRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(run -> TrainingRunResponse.from(run, null))
                .toList();
    }

    /**
     * Optional weekly run. Disabled by default, and it stops at the gate: it produces a
     * candidate version and never deploys one.
     */
    @Scheduled(cron = "${app.ml.training.cron:0 0 3 * * SUN}")
    public void scheduledRun() {
        if (!scheduledEnabled) {
            return;
        }
        try {
            TrainingRunResponse run = startRun("scheduler");
            log.info("Scheduled training run started: job {}", run.getJobId());
        } catch (ApiException ex) {
            log.info("Scheduled training run skipped: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Scheduled training run failed", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
