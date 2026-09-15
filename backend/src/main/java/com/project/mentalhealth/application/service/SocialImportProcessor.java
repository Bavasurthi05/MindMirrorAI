package com.project.mentalhealth.application.service;

import com.project.mentalhealth.infrastructure.async.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Analyzes an import's posts on the ML pool once the upload transaction has committed.
 *
 * <p>After-commit matters: the posts it reads are written by the upload, and must exist by the
 * time analysis starts.
 */
@Component
public class SocialImportProcessor {

    private static final Logger log = LoggerFactory.getLogger(SocialImportProcessor.class);

    private final SocialImportService socialImportService;

    public SocialImportProcessor(SocialImportService socialImportService) {
        this.socialImportService = socialImportService;
    }

    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onImportRequested(SocialImportRequestedEvent event) {
        try {
            socialImportService.analyzeImport(event.importId());
        } catch (Exception ex) {
            // Unanalyzed posts stay in place; failed analyses are picked up by the retry job.
            log.error("Analysis of social import {} failed", event.importId(), ex);
        }
    }
}
