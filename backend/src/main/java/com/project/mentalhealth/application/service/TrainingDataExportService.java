package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.out.MlTrainingPort;
import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.FeedbackAgreement;
import com.project.mentalhealth.domain.model.PredictionFeedback;
import com.project.mentalhealth.domain.model.UserPreferences;
import com.project.mentalhealth.domain.repository.PredictionFeedbackRepository;
import com.project.mentalhealth.domain.repository.UserPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns user feedback into a training corpus the ML service can consume.
 *
 * <p>This is the one place where user-written text leaves the backend, so every privacy
 * rule is enforced here rather than in the ML service:
 * <ul>
 *   <li>only users who explicitly opted in ({@link UserPreferences#isTrainingConsent()}),</li>
 *   <li>identifiers replaced by a salted hash — no email, name, or entry title ever leaves,</li>
 *   <li>PII scrubbed from the text itself,</li>
 *   <li>a per-user cap so one prolific user cannot dominate the corpus.</li>
 * </ul>
 */
@Service
public class TrainingDataExportService {

    private static final Logger log = LoggerFactory.getLogger(TrainingDataExportService.class);

    private static final int MIN_TEXT_LENGTH = 20;

    // Deliberately blunt: over-scrubbing costs a little signal, under-scrubbing leaks PII.
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern URL = Pattern.compile("https?://\\S+|www\\.\\S+");
    private static final Pattern HANDLE = Pattern.compile("[@#]\\w+");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\d)");
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<!\\d)\\d{6,}(?!\\d)");

    private final PredictionFeedbackRepository feedbackRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final String hashSalt;
    private final int maxPerUser;

    public TrainingDataExportService(PredictionFeedbackRepository feedbackRepository,
                                     UserPreferencesRepository preferencesRepository,
                                     @Value("${app.ml.training.hash-salt:mindmirror-local-salt}") String hashSalt,
                                     @Value("${app.ml.training.max-per-user:200}") int maxPerUser) {
        this.feedbackRepository = feedbackRepository;
        this.preferencesRepository = preferencesRepository;
        this.hashSalt = hashSalt;
        this.maxPerUser = maxPerUser;
    }

    /** Consent-filtered, scrubbed, pseudonymized examples ready for {@code /train/run}. */
    @Transactional(readOnly = true)
    public List<MlTrainingPort.TrainingExample> export() {
        Map<Long, Boolean> consentCache = new HashMap<>();
        Map<String, Integer> perUserCount = new HashMap<>();
        Set<String> seenText = new HashSet<>();
        List<MlTrainingPort.TrainingExample> examples = new ArrayList<>();

        int skippedNoConsent = 0;
        int skippedTooShort = 0;
        int skippedDuplicate = 0;
        int skippedCapped = 0;

        for (PredictionFeedback feedback : feedbackRepository.findAll()) {
            Long userId = feedback.getUser().getId();
            if (!consentCache.computeIfAbsent(userId, this::hasTrainingConsent)) {
                skippedNoConsent++;
                continue;
            }

            String label = labelFor(feedback);
            if (label == null) {
                continue;
            }

            AnalysisResult analysis = feedback.getAnalysisResult();
            String scrubbed = scrub(analysis.getSourceText());
            if (scrubbed == null || scrubbed.length() < MIN_TEXT_LENGTH) {
                skippedTooShort++;
                continue;
            }
            if (!seenText.add(scrubbed.toLowerCase())) {
                skippedDuplicate++;
                continue;
            }

            String userHash = hash(userId);
            int used = perUserCount.getOrDefault(userHash, 0);
            if (used >= maxPerUser) {
                skippedCapped++;
                continue;
            }
            perUserCount.put(userHash, used + 1);

            boolean corrected = feedback.getAgreement() == FeedbackAgreement.DISAGREE;
            examples.add(new MlTrainingPort.TrainingExample(
                    scrubbed,
                    label,
                    corrected ? "user_correction" : "user_agreement",
                    // A correction is a direct supervised label; an agreement only confirms
                    // what the model already believed, so it carries less weight.
                    corrected ? 1.0 : 0.6,
                    userHash));
        }

        log.info("Training export: {} examples (skipped: {} no-consent, {} too-short, {} duplicate, {} capped)",
                examples.size(), skippedNoConsent, skippedTooShort, skippedDuplicate, skippedCapped);
        return examples;
    }

    /** Counts without building the payload, for the admin readiness view. */
    @Transactional(readOnly = true)
    public ExportSummary summary() {
        List<MlTrainingPort.TrainingExample> examples = export();
        Map<String, Long> byLabel = new HashMap<>();
        for (MlTrainingPort.TrainingExample example : examples) {
            byLabel.merge(example.label(), 1L, Long::sum);
        }
        long consenting = preferencesRepository.findAll().stream()
                .filter(UserPreferences::isTrainingConsent)
                .count();
        return new ExportSummary(examples.size(), byLabel, consenting);
    }

    public record ExportSummary(int exportableExamples, Map<String, Long> labelCounts, long consentingUsers) {}

    /**
     * The label this feedback teaches.
     *
     * <p>A correction supplies it directly; an agreement confirms the predicted label.
     * A {@code PARTIAL} answer is deliberately not used — it is too ambiguous to train on.
     */
    private String labelFor(PredictionFeedback feedback) {
        if (feedback.getAgreement() == FeedbackAgreement.DISAGREE) {
            return feedback.getCorrectedLabel();
        }
        if (feedback.getAgreement() == FeedbackAgreement.AGREE) {
            return feedback.getPredictedLabel();
        }
        return null;
    }

    private boolean hasTrainingConsent(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserPreferences::isTrainingConsent)
                .orElse(false);
    }

    /** Strips the identifiers most likely to appear in free text. */
    static String scrub(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = EMAIL.matcher(text).replaceAll(" ");
        cleaned = URL.matcher(cleaned).replaceAll(" ");
        cleaned = HANDLE.matcher(cleaned).replaceAll(" ");
        cleaned = PHONE.matcher(cleaned).replaceAll(" ");
        cleaned = LONG_NUMBER.matcher(cleaned).replaceAll(" ");
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    /** Salted hash so exported rows can be rate-limited per user without identifying them. */
    private String hash(Long userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((hashSalt + ":" + userId).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                builder.append(String.format("%02x", bytes[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) { // pragma: SHA-256 is always present
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
