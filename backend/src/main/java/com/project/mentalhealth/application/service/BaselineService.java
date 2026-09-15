package com.project.mentalhealth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.model.JournalEntry;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.model.UserBaseline;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserBaselineRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes each user's own normal range.
 *
 * <p>Everything here is descriptive statistics over that one user's history — there is no
 * cross-user modelling, so a person's baseline never depends on anyone else's data.
 */
@Service
public class BaselineService {

    private static final Logger log = LoggerFactory.getLogger(BaselineService.class);

    /** Long enough to smooth out individual bad days without becoming unresponsive. */
    private static final int WINDOW_DAYS = 60;
    private static final int MAX_ANALYSES = 200;

    private final UserRepository userRepository;
    private final UserBaselineRepository baselineRepository;
    private final AnalysisResultRepository analysisRepository;
    private final MoodEntryRepository moodRepository;
    private final TriggerEntryRepository triggerRepository;
    private final JournalEntryRepository journalRepository;
    private final ObjectMapper objectMapper;
    /** Self-reference so REQUIRES_NEW actually goes through the transactional proxy. */
    private final ObjectProvider<BaselineService> self;

    public BaselineService(UserRepository userRepository,
                           UserBaselineRepository baselineRepository,
                           AnalysisResultRepository analysisRepository,
                           MoodEntryRepository moodRepository,
                           TriggerEntryRepository triggerRepository,
                           JournalEntryRepository journalRepository,
                           ObjectMapper objectMapper,
                           ObjectProvider<BaselineService> self) {
        this.userRepository = userRepository;
        this.baselineRepository = baselineRepository;
        this.analysisRepository = analysisRepository;
        this.moodRepository = moodRepository;
        this.triggerRepository = triggerRepository;
        this.journalRepository = journalRepository;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    /**
     * The stored baseline, recomputing it on demand when missing or stale.
     *
     * <p>{@code REQUIRES_NEW} on the recompute: this is reached from read-only request paths
     * (dashboard, reports), which cannot write. The refresh runs in its own transaction.
     */
    @Transactional(readOnly = true)
    public UserBaseline baselineFor(User user) {
        UserBaseline existing = baselineRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null && existing.getComputedAt() != null && !isStale(existing)) {
            return existing;
        }
        // Through the proxy, not `this`: a self-call would inherit the caller's read-only
        // transaction and the write would be rejected.
        return self.getObject().recompute(user);
    }

    /**
     * A settled baseline is refreshed nightly; one that is still forming is refreshed hourly,
     * so a new user's first days of data are reflected without waiting a full day.
     */
    private boolean isStale(UserBaseline baseline) {
        Instant threshold = baseline.isEstablished()
                ? Instant.now().minus(1, ChronoUnit.DAYS)
                : Instant.now().minus(1, ChronoUnit.HOURS);
        return baseline.getComputedAt().isBefore(threshold);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserBaseline recompute(User user) {
        Long userId = user.getId();
        Instant cutoff = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);

        List<AnalysisResult> analyses = analysisRepository
                .findByUserIdAndStatusAndSourceTypeNotAndAnalyzedAtAfterOrderByAnalyzedAtDesc(
                        userId, AnalysisStatus.OK, AnalysisSourceType.SOCIAL, cutoff)
                .stream()
                .limit(MAX_ANALYSES)
                .toList();
        List<MoodEntry> moods = moodRepository
                .findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(userId, cutoff);
        List<TriggerEntry> triggers = triggerRepository
                .findByUserIdAndConfirmationNotOrderByOccurredAtDesc(userId, TriggerConfirmation.DISMISSED)
                .stream()
                .filter(trigger -> trigger.getOccurredAt().isAfter(cutoff))
                .toList();
        List<JournalEntry> journals = journalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(entry -> entry.getCreatedAt() != null && entry.getCreatedAt().isAfter(cutoff))
                .toList();

        UserBaseline baseline = baselineRepository.findByUserId(userId).orElseGet(UserBaseline::new);
        baseline.setUser(user);

        List<Double> sentiments = analyses.stream()
                .map(AnalysisResult::getSentimentScore)
                .filter(java.util.Objects::nonNull)
                .toList();
        baseline.setMeanSentiment(mean(sentiments));
        baseline.setStdDevSentiment(stdDev(sentiments, baseline.getMeanSentiment()));

        List<Double> moodScores = moods.stream().map(mood -> (double) mood.getMoodScore()).toList();
        baseline.setMeanMood(mean(moodScores));
        baseline.setStdDevMood(stdDev(moodScores, baseline.getMeanMood()));

        baseline.setMeanTriggerIntensity(mean(
                triggers.stream().map(trigger -> (double) trigger.getIntensity()).toList()));

        long journalDays = journals.stream()
                .map(entry -> entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate())
                .distinct()
                .count();
        int daysCovered = daysCovered(analyses, moods, journals);
        baseline.setDaysCovered(daysCovered);
        baseline.setJournalingDaysPerWeek(daysCovered == 0 ? null
                : round(7.0 * journalDays / Math.max(daysCovered, 1)));

        baseline.setStateRates(toJson(stateRates(analyses)));
        baseline.setSampleSize(analyses.size() + moods.size());
        baseline.setEstablished(daysCovered >= UserBaseline.MIN_DAYS_FOR_BASELINE
                && baseline.getSampleSize() >= UserBaseline.MIN_SAMPLES_FOR_BASELINE);
        baseline.setComputedAt(Instant.now());

        return baselineRepository.save(baseline);
    }

    /**
     * Nightly refresh so the app is not recomputing baselines inside user requests.
     *
     * <p>Runs per user in its own transaction: one user's bad data must not abort everyone
     * else's recompute.
     */
    @Scheduled(cron = "${app.baseline.cron:0 30 2 * * *}")
    public void recomputeAll() {
        List<User> users = userRepository.findAll();
        int failures = 0;
        for (User user : users) {
            try {
                self.getObject().recompute(user);
            } catch (Exception ex) {
                failures++;
                log.warn("Baseline recompute failed for user {}: {}", user.getId(), ex.getMessage());
            }
        }
        log.info("Recomputed baselines for {} users ({} failures)", users.size() - failures, failures);
    }

    // --- Statistics ---------------------------------------------------------------------

    static Double mean(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        return round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    /** Population standard deviation; null below two points, where spread is undefined. */
    static Double stdDev(List<Double> values, Double mean) {
        if (values.size() < 2 || mean == null) {
            return null;
        }
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0);
        return round(Math.sqrt(variance));
    }

    /**
     * How far a value sits from the user's own normal, in standard deviations.
     *
     * <p>Returns null when there is no usable spread — with a flat history any deviation
     * would divide by ~zero and produce a meaningless spike.
     */
    static Double zScore(Double value, Double mean, Double stdDev) {
        if (value == null || mean == null || stdDev == null || stdDev < 0.01) {
            return null;
        }
        return round((value - mean) / stdDev);
    }

    private Map<String, Double> stateRates(List<AnalysisResult> analyses) {
        Map<String, Double> rates = new HashMap<>();
        if (analyses.isEmpty()) {
            return rates;
        }
        Map<String, Long> counts = new HashMap<>();
        for (AnalysisResult analysis : analyses) {
            if (analysis.getPrediction() != null) {
                counts.merge(analysis.getPrediction(), 1L, Long::sum);
            }
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return rates;
        }
        counts.forEach((state, count) -> rates.put(state, round((double) count / total)));
        return rates;
    }

    private int daysCovered(List<AnalysisResult> analyses, List<MoodEntry> moods, List<JournalEntry> journals) {
        Instant earliest = null;
        for (AnalysisResult analysis : analyses) {
            earliest = earlier(earliest, analysis.getAnalyzedAt());
        }
        for (MoodEntry mood : moods) {
            earliest = earlier(earliest, mood.getRecordedAt());
        }
        for (JournalEntry journal : journals) {
            earliest = earlier(earliest, journal.getCreatedAt());
        }
        if (earliest == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(earliest, Instant.now()) + 1;
    }

    private Instant earlier(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isBefore(current) ? candidate : current;
    }

    private static Double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
