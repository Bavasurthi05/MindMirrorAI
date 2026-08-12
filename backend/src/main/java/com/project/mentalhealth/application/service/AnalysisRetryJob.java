package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reprocesses analyses that never completed — typically because the ML service was down
 * when the user wrote the entry.
 *
 * <p>Without this, an ML outage would leave permanently blank analyses on entries the user
 * has already written and moved on from.
 */
@Component
public class AnalysisRetryJob {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRetryJob.class);

    private final AnalysisResultRepository analysisRepository;
    private final AnalysisOrchestrator orchestrator;
    private final int maxAttempts;
    private final int batchSize;

    public AnalysisRetryJob(AnalysisResultRepository analysisRepository,
                            AnalysisOrchestrator orchestrator,
                            @Value("${app.analysis.max-attempts:3}") int maxAttempts,
                            @Value("${app.analysis.retry-batch-size:25}") int batchSize) {
        this.analysisRepository = analysisRepository;
        this.orchestrator = orchestrator;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.analysis.retry-interval-ms:120000}",
            initialDelayString = "${app.analysis.retry-initial-delay-ms:60000}")
    public void retryIncomplete() {
        List<AnalysisResult> stuck = analysisRepository.findByStatusAndAttemptCountLessThanOrderByIdAsc(
                AnalysisStatus.PENDING, maxAttempts, PageRequest.of(0, batchSize));
        List<AnalysisResult> failed = analysisRepository.findByStatusAndAttemptCountLessThanOrderByIdAsc(
                AnalysisStatus.FAILED, maxAttempts, PageRequest.of(0, batchSize));

        List<AnalysisResult> candidates = new java.util.ArrayList<>(stuck);
        candidates.addAll(failed);
        if (candidates.isEmpty()) {
            return;
        }

        log.info("Retrying {} incomplete analyses", candidates.size());
        for (AnalysisResult candidate : candidates) {
            try {
                orchestrator.process(candidate.getId());
            } catch (Exception ex) {
                // Attempt count was already incremented; stop early if the service is still down.
                log.warn("Retry of analysis {} failed: {}", candidate.getId(), ex.getMessage());
                return;
            }
        }
    }
}
