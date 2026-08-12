package com.project.mentalhealth.application.service;

import com.project.mentalhealth.infrastructure.async.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs queued analyses on the ML pool once the submitting transaction has committed.
 *
 * <p>Separate bean by necessity: {@code @Async} is applied by a proxy, so the orchestrator
 * calling itself would run inline on the request thread.
 */
@Component
public class AnalysisEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnalysisEventListener.class);

    private final AnalysisOrchestrator orchestrator;

    public AnalysisEventListener(AnalysisOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAnalysisRequested(AnalysisRequestedEvent event) {
        try {
            orchestrator.process(event.analysisId());
        } catch (Exception ex) {
            // The row stays PENDING/FAILED and the retry job will pick it up.
            log.error("Background analysis {} failed", event.analysisId(), ex);
        }
    }
}
