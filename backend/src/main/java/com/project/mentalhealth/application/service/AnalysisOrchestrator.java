package com.project.mentalhealth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.model.EntrySource;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Single entry point for analyzing user text and persisting the result.
 *
 * <p>The write path is deliberately two-phase:
 * <ol>
 *   <li>{@link #submit} inserts a {@code PENDING} row inside the caller's transaction and returns
 *       immediately — a journal save never waits on, or fails because of, the ML service.</li>
 *   <li>A pool thread calls the ML service and completes the row, deriving mood and trigger
 *       entries from the result so downstream analytics have real data to work with.</li>
 * </ol>
 */
@Service
public class AnalysisOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrator.class);

    /** Below this, derived triggers are noise rather than signal. */
    private static final int MIN_DERIVED_TRIGGER_INTENSITY = 3;

    private final MlAnalysisPort mlAnalysisPort;
    private final AnalysisResultRepository analysisRepository;
    private final MoodEntryRepository moodRepository;
    private final TriggerEntryRepository triggerRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean deriveMoodEntries;
    private final boolean deriveTriggerEntries;

    public AnalysisOrchestrator(MlAnalysisPort mlAnalysisPort,
                                AnalysisResultRepository analysisRepository,
                                MoodEntryRepository moodRepository,
                                TriggerEntryRepository triggerRepository,
                                ObjectMapper objectMapper,
                                ApplicationEventPublisher eventPublisher,
                                @Value("${app.analysis.derive-mood-entries:true}") boolean deriveMoodEntries,
                                @Value("${app.analysis.derive-trigger-entries:true}") boolean deriveTriggerEntries) {
        this.mlAnalysisPort = mlAnalysisPort;
        this.analysisRepository = analysisRepository;
        this.moodRepository = moodRepository;
        this.triggerRepository = triggerRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.deriveMoodEntries = deriveMoodEntries;
        this.deriveTriggerEntries = deriveTriggerEntries;
    }

    /**
     * Queue text for analysis. Returns the persisted {@code PENDING} row.
     *
     * <p>Safe to call from inside a transaction: the row is written with the caller, and the ML
     * call happens after the caller commits.
     */
    @Transactional
    public AnalysisResult submit(User user, AnalysisSourceType sourceType, Long sourceId, String text) {
        AnalysisResult pending = new AnalysisResult();
        pending.setUser(user);
        pending.setSourceType(sourceType);
        pending.setSourceId(sourceId);
        pending.setSourceText(text);
        pending.setStatus(AnalysisStatus.PENDING);
        AnalysisResult saved = analysisRepository.save(pending);
        // Published, not called directly: the listener is @Async and fires after this
        // transaction commits, so the ML worker can never read a row that isn't there yet.
        eventPublisher.publishEvent(new AnalysisRequestedEvent(saved.getId()));
        return saved;
    }

    /**
     * Analyze synchronously and return the completed row.
     *
     * <p>For callers that need the result in the response (an explicit "Analyze" action). Still
     * persists, so the result survives a page refresh.
     */
    @Transactional
    public CompletedAnalysis submitAndWait(User user, AnalysisSourceType sourceType, Long sourceId, String text) {
        AnalysisResult pending = new AnalysisResult();
        pending.setUser(user);
        pending.setSourceType(sourceType);
        pending.setSourceId(sourceId);
        pending.setSourceText(text);
        pending.setStatus(AnalysisStatus.PENDING);
        AnalysisResult saved = analysisRepository.save(pending);
        MlAnalysisPort.JournalAnalysis analysis = complete(saved);
        return new CompletedAnalysis(analysisRepository.save(saved), analysis);
    }

    /** A stored analysis paired with the ML payload that produced it. */
    public record CompletedAnalysis(AnalysisResult result, MlAnalysisPort.JournalAnalysis analysis) {
        public boolean isOk() {
            return result.getStatus() == AnalysisStatus.OK && analysis != null;
        }
    }

    /**
     * Run one pending analysis to completion in its own transaction.
     *
     * <p>{@code REQUIRES_NEW} because this runs after the submitting transaction has committed;
     * it must not join a stale one.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long analysisId) {
        AnalysisResult result = analysisRepository.findById(analysisId).orElse(null);
        if (result == null) {
            log.warn("Analysis {} disappeared before processing", analysisId);
            return;
        }
        if (result.getStatus() == AnalysisStatus.OK) {
            return;
        }
        complete(result);
        analysisRepository.save(result);
    }

    /**
     * Calls the ML service and populates the row.
     *
     * @return the analysis, or null when the call failed (recorded on the row, never thrown)
     */
    private MlAnalysisPort.JournalAnalysis complete(AnalysisResult result) {
        result.setAttemptCount(result.getAttemptCount() + 1);
        try {
            MlAnalysisPort.JournalAnalysis analysis = result.getSourceType() == AnalysisSourceType.SOCIAL
                    ? mlAnalysisPort.analyzeSocial(result.getSourceText())
                    : mlAnalysisPort.analyzeJournal(result.getSourceText());
            apply(result, analysis);
            deriveSignals(result, analysis);
            return analysis;
        } catch (Exception ex) {
            result.setStatus(AnalysisStatus.FAILED);
            result.setErrorMessage(truncate(ex.getMessage()));
            log.warn("Analysis {} failed on attempt {}: {}",
                    result.getId(), result.getAttemptCount(), ex.getMessage());
            return null;
        }
    }

    private void apply(AnalysisResult result, MlAnalysisPort.JournalAnalysis analysis) {
        result.setSentiment(analysis.sentiment());
        result.setSentimentScore(analysis.sentimentScore());
        result.setDominantEmotion(analysis.emotion());
        result.setEmotionScores(toJson(analysis.emotionScores()));
        result.setPrediction(analysis.prediction());
        result.setPredictionConfidence(analysis.predictionConfidence());
        result.setPredictionProbabilities(toJson(analysis.predictionProbabilities()));
        result.setReasons(toJson(analysis.reasons()));
        result.setExplanation(toJson(analysis.explanation()));
        result.setDetectedTriggers(toJson(analysis.triggers()));
        result.setModelBackend(analysis.modelBackend());
        result.setModelVersion(analysis.modelVersion());
        result.setAnalyzedAt(Instant.now());
        result.setErrorMessage(null);
        result.setStatus(AnalysisStatus.OK);
    }

    /**
     * Turn an analysis into the mood and trigger rows that analytics reads.
     *
     * <p>This is what makes the dashboard populate from ordinary journaling: previously nothing
     * in the app ever wrote {@code mood_entries} or {@code trigger_entries}, so every chart was
     * empty. Derived rows are tagged {@link EntrySource#DERIVED} and never overwrite a
     * self-reported entry for the same day.
     */
    private void deriveSignals(AnalysisResult result, MlAnalysisPort.JournalAnalysis analysis) {
        Long userId = result.getUser().getId();
        Instant analyzedAt = result.getAnalyzedAt();

        if (deriveMoodEntries && !hasSelfReportedMoodOn(userId, analyzedAt)) {
            MoodEntry mood = new MoodEntry();
            mood.setUser(result.getUser());
            mood.setMoodScore(toMoodScore(analysis.sentimentScore()));
            mood.setMoodLabel(analysis.emotion());
            mood.setRecordedAt(analyzedAt);
            mood.setSource(EntrySource.DERIVED);
            moodRepository.save(mood);
        }

        if (deriveTriggerEntries) {
            for (MlAnalysisPort.DetectedTrigger trigger : analysis.triggers()) {
                if (trigger.intensity() < MIN_DERIVED_TRIGGER_INTENSITY) {
                    continue;
                }
                TriggerEntry entry = new TriggerEntry();
                entry.setUser(result.getUser());
                entry.setCategory(trigger.category());
                entry.setIntensity(trigger.intensity());
                entry.setNote("Detected in your writing: " + String.join(", ", trigger.matchedTerms()));
                entry.setOccurredAt(analyzedAt);
                entry.setSource(EntrySource.DERIVED);
                // Shown straight away, but flagged for review: confirming teaches us it was right,
                // dismissing removes it from analytics and records that the detection was wrong.
                entry.setConfirmation(TriggerConfirmation.PENDING);
                entry.setAnalysisResultId(result.getId());
                triggerRepository.save(entry);
            }
        }
    }

    /** A self-reported mood for the day wins; we do not stack a derived one on top of it. */
    private boolean hasSelfReportedMoodOn(Long userId, Instant when) {
        LocalDate day = when.atZone(ZoneOffset.UTC).toLocalDate();
        List<MoodEntry> sameDay = moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(
                userId, day.atStartOfDay(ZoneOffset.UTC).toInstant());
        return sameDay.stream()
                .anyMatch(entry -> EntrySource.SELF_REPORTED.equals(entry.getSource())
                        && entry.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(day));
    }

    /** Sentiment runs -1..1; mood scores are stored 0..100. */
    static int toMoodScore(double sentimentScore) {
        double clamped = Math.max(-1.0, Math.min(1.0, sentimentScore));
        return (int) Math.round((clamped + 1.0) / 2.0 * 100);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize analysis field: {}", ex.getMessage());
            return null;
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
