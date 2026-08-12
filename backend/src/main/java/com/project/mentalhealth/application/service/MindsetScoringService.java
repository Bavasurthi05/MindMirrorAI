package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.model.AssessmentSubmission;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.RecoveryAction;
import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.model.UserBaseline;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.MindsetResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The single wellbeing number the whole app agrees on.
 *
 * <p>Three properties make it defensible for a non-clinical tool:
 * <ul>
 *   <li><b>Composite</b> — no one signal dominates.</li>
 *   <li><b>Explainable</b> — every component's contribution is returned, so the number can
 *       always be broken down into why.</li>
 *   <li><b>Honest about absence</b> — missing components are dropped and the remaining
 *       weights renormalized; below two components the score is null, not a guess.</li>
 * </ul>
 *
 * <p>The score is compared against the user's own {@link UserBaseline}, so "low" means low
 * for them rather than low against a population they never consented to be measured by.
 */
@Service
public class MindsetScoringService {

    // Sum to 1.0; renormalized whenever a component is unavailable.
    static final double WEIGHT_SENTIMENT = 0.25;
    static final double WEIGHT_MOOD = 0.25;
    static final double WEIGHT_TRIGGERS = 0.20;
    static final double WEIGHT_ASSESSMENT = 0.20;
    static final double WEIGHT_RECOVERY = 0.10;

    /** One component alone is a single signal wearing a composite's clothes. */
    static final int MIN_COMPONENTS = 2;

    private static final int WINDOW_DAYS = 14;

    private final UserRepository userRepository;
    private final AnalysisResultRepository analysisRepository;
    private final MoodEntryRepository moodRepository;
    private final TriggerEntryRepository triggerRepository;
    private final AssessmentSubmissionRepository assessmentRepository;
    private final RecoveryActionRepository recoveryRepository;
    private final BaselineService baselineService;

    public MindsetScoringService(UserRepository userRepository,
                                 AnalysisResultRepository analysisRepository,
                                 MoodEntryRepository moodRepository,
                                 TriggerEntryRepository triggerRepository,
                                 AssessmentSubmissionRepository assessmentRepository,
                                 RecoveryActionRepository recoveryRepository,
                                 BaselineService baselineService) {
        this.userRepository = userRepository;
        this.analysisRepository = analysisRepository;
        this.moodRepository = moodRepository;
        this.triggerRepository = triggerRepository;
        this.assessmentRepository = assessmentRepository;
        this.recoveryRepository = recoveryRepository;
        this.baselineService = baselineService;
    }

    @Transactional
    public MindsetResponse mindset(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return mindsetFor(user);
    }

    @Transactional
    public MindsetResponse mindsetFor(User user) {
        Long userId = user.getId();
        Instant cutoff = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);

        List<AnalysisResult> analyses = analysisRepository
                .findByUserIdAndStatusAndAnalyzedAtAfterOrderByAnalyzedAtDesc(userId, AnalysisStatus.OK, cutoff);
        List<MoodEntry> moods = moodRepository
                .findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(userId, cutoff);
        List<TriggerEntry> triggers = triggerRepository
                .findByUserIdAndConfirmationNotOrderByOccurredAtDesc(userId, TriggerConfirmation.DISMISSED)
                .stream()
                .filter(trigger -> trigger.getOccurredAt().isAfter(cutoff))
                .toList();
        List<AssessmentSubmission> assessments = assessmentRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        List<RecoveryAction> recoveryActions = recoveryRepository.findByUserIdOrderByIdAsc(userId);

        List<Component> components = new ArrayList<>();

        Double sentiment = averageSentiment(analyses);
        if (sentiment != null) {
            // Sentiment runs -1..1; the score scale is 0..100.
            components.add(new Component("Reflection tone", WEIGHT_SENTIMENT,
                    clamp((sentiment + 1) / 2 * 100),
                    analyses.size() + " analyzed entr" + (analyses.size() == 1 ? "y" : "ies")));
        }

        if (!moods.isEmpty()) {
            double averageMood = moods.stream().mapToInt(MoodEntry::getMoodScore).average().orElse(0);
            components.add(new Component("Mood", WEIGHT_MOOD, clamp(averageMood),
                    moods.size() + " check-in" + (moods.size() == 1 ? "" : "s")));
        }

        if (!triggers.isEmpty()) {
            double averageIntensity = triggers.stream().mapToInt(TriggerEntry::getIntensity).average().orElse(0);
            // Inverted: heavier trigger load lowers the score.
            components.add(new Component("Trigger load", WEIGHT_TRIGGERS,
                    clamp(100 - averageIntensity * 10),
                    triggers.size() + " trigger" + (triggers.size() == 1 ? "" : "s")));
        }

        AssessmentSubmission latest = assessments.stream().findFirst().orElse(null);
        if (latest != null && latest.getMaxScore() > 0) {
            components.add(new Component("Questionnaire", WEIGHT_ASSESSMENT,
                    clamp(100.0 * latest.getTotalScore() / latest.getMaxScore()),
                    "latest: " + latest.getSeverity()));
        }

        if (!recoveryActions.isEmpty()) {
            long completed = recoveryActions.stream().filter(RecoveryAction::isCompleted).count();
            components.add(new Component("Recovery engagement", WEIGHT_RECOVERY,
                    clamp(100.0 * completed / recoveryActions.size()),
                    completed + "/" + recoveryActions.size() + " actions done"));
        }

        UserBaseline baseline = baselineService.baselineFor(user);
        Integer score = compose(components);
        Double deviation = BaselineService.zScore(sentiment,
                baseline.getMeanSentiment(), baseline.getStdDevSentiment());

        return MindsetResponse.builder()
                .score(score)
                .band(band(score))
                .components(components.stream()
                        .map(component -> MindsetResponse.Component.builder()
                                .label(component.label())
                                .value((int) Math.round(component.value()))
                                .weight(round(normalizedWeight(component, components)))
                                .contribution(round(component.value() * normalizedWeight(component, components)))
                                .detail(component.detail())
                                .build())
                        .toList())
                .confidence(confidence(components, baseline))
                .baselineEstablished(baseline.isEstablished())
                .baselineDaysCovered(baseline.getDaysCovered())
                .baselineMeanSentiment(baseline.getMeanSentiment())
                .baselineMeanMood(baseline.getMeanMood())
                .deviationSigma(deviation)
                .insight(insight(score, deviation, baseline, components.size()))
                .build();
    }

    /**
     * Weighted average over available components, with weights renormalized.
     *
     * @return null when fewer than {@link #MIN_COMPONENTS} are available
     */
    static Integer compose(List<Component> components) {
        if (components.size() < MIN_COMPONENTS) {
            return null;
        }
        double totalWeight = components.stream().mapToDouble(Component::weight).sum();
        if (totalWeight <= 0) {
            return null;
        }
        double weighted = components.stream()
                .mapToDouble(component -> component.value() * component.weight())
                .sum();
        return (int) Math.round(weighted / totalWeight);
    }

    private static double normalizedWeight(Component component, List<Component> components) {
        double totalWeight = components.stream().mapToDouble(Component::weight).sum();
        return totalWeight <= 0 ? 0 : component.weight() / totalWeight;
    }

    static String band(Integer score) {
        if (score == null) {
            return null;
        }
        if (score >= 75) return "Thriving";
        if (score >= 60) return "Steady";
        if (score >= 40) return "Strained";
        return "Struggling";
    }

    /** Confidence reflects how much evidence stands behind the number, not model certainty. */
    static double confidence(List<Component> components, UserBaseline baseline) {
        if (components.size() < MIN_COMPONENTS) {
            return 0.0;
        }
        double coverage = Math.min(1.0, components.size() / 5.0);
        double history = baseline != null && baseline.isEstablished() ? 1.0
                : Math.min(1.0, (baseline == null ? 0 : baseline.getDaysCovered())
                / (double) UserBaseline.MIN_DAYS_FOR_BASELINE);
        return round(0.4 * coverage + 0.6 * history);
    }

    /**
     * Deviation-framed copy: "unusual for you" rather than a bare clinical-sounding label.
     */
    static String insight(Integer score, Double deviation, UserBaseline baseline, int componentCount) {
        if (score == null) {
            return "Add a couple more entries and we can put a number on how things are going.";
        }
        if (baseline == null || !baseline.isEstablished()) {
            return "Still learning what's normal for you — keep going and this will get more personal.";
        }
        if (deviation == null) {
            return "Your recent entries look consistent with your usual range.";
        }
        double magnitude = Math.abs(deviation);
        if (magnitude < 1.0) {
            return "This is within your usual range.";
        }
        String direction = deviation < 0 ? "below" : "above";
        return String.format(Locale.ENGLISH,
                "Your reflection tone this fortnight is %.1f standard deviations %s your usual range.",
                magnitude, direction);
    }

    private Double averageSentiment(List<AnalysisResult> analyses) {
        List<Double> scores = analyses.stream()
                .map(AnalysisResult::getSentimentScore)
                .filter(java.util.Objects::nonNull)
                .toList();
        return scores.isEmpty() ? null
                : scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** One weighted input to the composite. */
    record Component(String label, double weight, double value, String detail) {}
}
