package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.model.AssessmentSubmission;
import com.project.mentalhealth.domain.model.EntrySource;
import com.project.mentalhealth.domain.model.JournalEntry;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.RecoveryAction;
import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.DashboardResponse;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.MindsetResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Builds the dashboard entirely from the signed-in user's own data.
 *
 * <p>Every figure here traces back to something the user did. Where a figure cannot be
 * computed the field is null and the client renders an empty state — the dashboard previously
 * showed hardcoded scores that looked real, which is worse than showing nothing.
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MOOD_WEEK_DAYS = 7;
    private static final int RECENT_WINDOW_DAYS = 14;
    private static final int RECENT_ANALYSIS_COUNT = 3;
    private static final int TOP_TRIGGER_COUNT = 3;
    private static final int MAX_RECOMMENDATIONS = 3;
    private static final int MIN_ENTRIES_FOR_STABILITY = 3;

    private final UserRepository userRepository;
    private final MoodEntryRepository moodRepository;
    private final TriggerEntryRepository triggerRepository;
    private final AssessmentSubmissionRepository assessmentRepository;
    private final RecoveryActionRepository recoveryRepository;
    private final JournalEntryRepository journalRepository;
    private final AnalysisResultRepository analysisRepository;
    private final UserPreferencesService preferencesService;
    private final CheckInService checkInService;
    private final MindsetScoringService mindsetScoringService;

    public DashboardService(UserRepository userRepository,
                            MoodEntryRepository moodRepository,
                            TriggerEntryRepository triggerRepository,
                            AssessmentSubmissionRepository assessmentRepository,
                            RecoveryActionRepository recoveryRepository,
                            JournalEntryRepository journalRepository,
                            AnalysisResultRepository analysisRepository,
                            UserPreferencesService preferencesService,
                            CheckInService checkInService,
                            MindsetScoringService mindsetScoringService) {
        this.userRepository = userRepository;
        this.moodRepository = moodRepository;
        this.triggerRepository = triggerRepository;
        this.assessmentRepository = assessmentRepository;
        this.recoveryRepository = recoveryRepository;
        this.journalRepository = journalRepository;
        this.analysisRepository = analysisRepository;
        this.preferencesService = preferencesService;
        this.checkInService = checkInService;
        this.mindsetScoringService = mindsetScoringService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        Long userId = user.getId();
        // Every daily bucket below is computed in the user's own timezone: in UTC the day
        // boundary lands mid-evening for much of the world, splitting a single day in two.
        ZoneId zone = preferencesService.zoneFor(userId);

        List<MoodEntry> moods = moodRepository.findByUserIdOrderByRecordedAtDesc(userId);
        List<TriggerEntry> triggers = triggerRepository
                .findByUserIdAndConfirmationNotOrderByOccurredAtDesc(userId, TriggerConfirmation.DISMISSED);
        int pendingTriggers = triggerRepository
                .findByUserIdAndConfirmationOrderByOccurredAtDesc(userId, TriggerConfirmation.PENDING).size();
        List<AssessmentSubmission> assessments = assessmentRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        List<RecoveryAction> recoveryActions = recoveryRepository.findByUserIdOrderByIdAsc(userId);
        List<JournalEntry> journals = journalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        // Imported social posts have their own view; left in, a bulk import would fill these cards.
        List<AnalysisResult> analyses = analysisRepository.findByUserIdAndStatusAndSourceTypeNotOrderByAnalyzedAtDesc(
                userId, AnalysisStatus.OK, AnalysisSourceType.SOCIAL, PageRequest.of(0, RECENT_ANALYSIS_COUNT));

        // One number across dashboard, mirror and reports: they previously each computed
        // their own "wellness" and could disagree with one another.
        MindsetResponse mindset = mindsetScoringService.mindsetFor(user);
        Integer wellness = mindset.getScore();

        return DashboardResponse.builder()
                .wellnessScore(wellness)
                .wellnessDelta(computeDelta(moods, zone))
                .wellnessUpdatedAt(latestActivity(moods, analyses))
                .headline(buildHeadline(journals, moods))
                .summary(buildSummary(moods, triggers, journals, zone))
                .moodWeek(buildMoodWeek(moods, zone))
                .recentAnalyses(buildRecentAnalyses(analyses, zone))
                .triggerSummary(buildTriggerSummary(triggers))
                .recommendations(buildRecommendations(recoveryActions))
                .progressItems(buildProgress(moods, journals, recoveryActions, zone))
                .dataCompleteness(DashboardResponse.DataCompleteness.builder()
                        .hasJournal(!journals.isEmpty())
                        .hasMood(!moods.isEmpty())
                        .hasAssessment(!assessments.isEmpty())
                        .hasTriggers(!triggers.isEmpty())
                        .hasAnalysis(!analyses.isEmpty())
                        .daysOfData(daysOfData(journals, moods))
                        .pendingAnalyses(analysisRepository.countByUserIdAndStatusAndSourceTypeNot(
                                userId, AnalysisStatus.PENDING, AnalysisSourceType.SOCIAL))
                        .onboardingCompleted(preferencesService.preferencesOrDefaults(user).isOnboardingCompleted())
                        .daysSinceAssessment(daysSinceAssessment(assessments))
                        .build())
                .mindsetBand(mindset.getBand())
                .mindsetInsight(mindset.getInsight())
                .checkIn(buildCheckIn(user, moods, zone))
                .pendingTriggerCount(pendingTriggers)
                .build();
    }

    private DashboardResponse.CheckIn buildCheckIn(User user, List<MoodEntry> moods, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        MoodEntry todays = moods.stream()
                .filter(mood -> EntrySource.SELF_REPORTED.equals(mood.getSource()))
                .filter(mood -> mood.getRecordedAt().atZone(zone).toLocalDate().equals(today))
                .findFirst()
                .orElse(null);
        return DashboardResponse.CheckIn.builder()
                .checkedInToday(todays != null)
                .streak(checkInService.checkInStreak(user.getId(), zone))
                .todaysScore(todays == null ? null : todays.getMoodScore())
                .build();
    }

    // --- Wellness ---------------------------------------------------------------------

    /** Percentage-point change between this week's and last week's mood average. */
    private Integer computeDelta(List<MoodEntry> moods, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        OptionalDouble current = averageBetween(moods, today.minusDays(6), today, zone);
        OptionalDouble previous = averageBetween(moods, today.minusDays(13), today.minusDays(7), zone);
        if (current.isEmpty() || previous.isEmpty()) {
            return null;
        }
        return (int) Math.round(current.getAsDouble() - previous.getAsDouble());
    }

    private Instant latestActivity(List<MoodEntry> moods, List<AnalysisResult> analyses) {
        Instant latestMood = moods.stream().findFirst().map(MoodEntry::getRecordedAt).orElse(null);
        Instant latestAnalysis = analyses.stream().findFirst().map(AnalysisResult::getAnalyzedAt).orElse(null);
        if (latestMood == null) {
            return latestAnalysis;
        }
        if (latestAnalysis == null) {
            return latestMood;
        }
        return latestMood.isAfter(latestAnalysis) ? latestMood : latestAnalysis;
    }

    private String buildHeadline(List<JournalEntry> journals, List<MoodEntry> moods) {
        long entriesThisWeek = journals.stream()
                .filter(entry -> entry.getCreatedAt() != null
                        && entry.getCreatedAt().isAfter(Instant.now().minus(7, ChronoUnit.DAYS)))
                .count();
        if (entriesThisWeek == 0 && moods.isEmpty()) {
            return "Let's start with your first reflection.";
        }
        if (entriesThisWeek == 0) {
            return "No reflections yet this week — a short entry keeps your insights current.";
        }
        return "You've logged " + entriesThisWeek + " reflection" + (entriesThisWeek == 1 ? "" : "s") + " this week.";
    }

    // --- Summary bands ----------------------------------------------------------------

    private DashboardResponse.Summary buildSummary(List<MoodEntry> moods,
                                                   List<TriggerEntry> triggers,
                                                   List<JournalEntry> journals,
                                                   ZoneId zone) {
        List<TriggerEntry> recentTriggers = triggers.stream()
                .filter(trigger -> trigger.getOccurredAt().isAfter(Instant.now().minus(RECENT_WINDOW_DAYS, ChronoUnit.DAYS)))
                .toList();
        String stress = null;
        if (!recentTriggers.isEmpty()) {
            double average = recentTriggers.stream().mapToInt(TriggerEntry::getIntensity).average().orElse(0);
            stress = average >= 7 ? "High" : average >= 4 ? "Medium" : "Low";
        }

        String energy = null;
        OptionalDouble moodAverage = moodsWithin(moods, RECENT_WINDOW_DAYS).stream()
                .mapToInt(MoodEntry::getMoodScore).average();
        if (moodAverage.isPresent()) {
            double value = moodAverage.getAsDouble();
            energy = value >= 70 ? "High" : value >= 45 ? "Moderate" : "Low";
        }

        String reflection = null;
        long journalDays = distinctJournalDays(journals, RECENT_WINDOW_DAYS, zone);
        if (journalDays > 0) {
            reflection = journalDays >= 8 ? "Consistent" : journalDays >= 4 ? "Steady" : "Building";
        }

        return DashboardResponse.Summary.builder()
                .stress(stress)
                .energy(energy)
                .reflection(reflection)
                .build();
    }

    // --- Charts and lists -------------------------------------------------------------

    private List<DashboardResponse.MoodDay> buildMoodWeek(List<MoodEntry> moods, ZoneId zone) {
        Map<LocalDate, List<Integer>> byDay = new LinkedHashMap<>();
        for (MoodEntry mood : moods) {
            LocalDate day = mood.getRecordedAt().atZone(zone).toLocalDate();
            byDay.computeIfAbsent(day, key -> new ArrayList<>()).add(mood.getMoodScore());
        }

        LocalDate today = LocalDate.now(zone);
        List<DashboardResponse.MoodDay> week = new ArrayList<>();
        for (int offset = MOOD_WEEK_DAYS - 1; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            List<Integer> scores = byDay.get(day);
            Integer score = (scores == null || scores.isEmpty()) ? null
                    : (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
            week.add(DashboardResponse.MoodDay.builder()
                    .date(day.format(ISO_DATE))
                    .label(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .score(score)
                    .build());
        }
        return week;
    }

    private List<DashboardResponse.RecentAnalysis> buildRecentAnalyses(List<AnalysisResult> analyses, ZoneId zone) {
        return analyses.stream()
                .map(analysis -> DashboardResponse.RecentAnalysis.builder()
                        .id(analysis.getId())
                        .date(analysis.getAnalyzedAt() == null ? null
                                : analysis.getAnalyzedAt().atZone(zone).toLocalDate().format(ISO_DATE))
                        .headline(analysisHeadline(analysis, zone))
                        .detail(analysisDetail(analysis))
                        .prediction(analysis.getPrediction())
                        .sentiment(analysis.getSentiment())
                        .build())
                .toList();
    }

    private String analysisHeadline(AnalysisResult analysis, ZoneId zone) {
        String emotion = analysis.getDominantEmotion() == null ? "neutral" : analysis.getDominantEmotion();
        String day = analysis.getAnalyzedAt() == null ? "A recent"
                : analysis.getAnalyzedAt().atZone(zone).getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return day + " reflection read as " + emotion + ".";
    }

    private String analysisDetail(AnalysisResult analysis) {
        String prediction = analysis.getPrediction() == null ? "unclassified" : analysis.getPrediction();
        if (analysis.getPredictionConfidence() == null) {
            return "Signals point to " + prediction + ".";
        }
        return String.format(Locale.ENGLISH, "Signals point to %s (%.0f%% confidence).",
                prediction, analysis.getPredictionConfidence() * 100);
    }

    private List<DashboardResponse.Labelled> buildTriggerSummary(List<TriggerEntry> triggers) {
        Map<String, Double> byCategory = triggers.stream()
                .collect(Collectors.groupingBy(TriggerEntry::getCategory,
                        Collectors.averagingInt(TriggerEntry::getIntensity)));
        return byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TOP_TRIGGER_COUNT)
                .map(entry -> DashboardResponse.Labelled.builder()
                        .label(entry.getKey())
                        .value(entry.getValue() >= 7 ? "High" : entry.getValue() >= 4 ? "Medium" : "Low")
                        .build())
                .toList();
    }

    private List<DashboardResponse.Recommendation> buildRecommendations(List<RecoveryAction> actions) {
        return actions.stream()
                .filter(action -> !action.isCompleted())
                .limit(MAX_RECOMMENDATIONS)
                .map(action -> DashboardResponse.Recommendation.builder()
                        .id(action.getId())
                        .title(action.getTitle())
                        .detail(action.getDescription())
                        .build())
                .toList();
    }

    private List<DashboardResponse.Progress> buildProgress(List<MoodEntry> moods,
                                                           List<JournalEntry> journals,
                                                           List<RecoveryAction> actions,
                                                           ZoneId zone) {
        List<DashboardResponse.Progress> items = new ArrayList<>();
        items.add(DashboardResponse.Progress.builder()
                .label("Mood stability")
                .progress(moodStability(moods))
                .build());
        items.add(DashboardResponse.Progress.builder()
                .label("Reflection consistency")
                .progress(journals.isEmpty() ? null
                        : (int) Math.round(100.0 * distinctJournalDays(journals, RECENT_WINDOW_DAYS, zone) / RECENT_WINDOW_DAYS))
                .build());
        items.add(DashboardResponse.Progress.builder()
                .label("Recovery habits")
                .progress(actions.isEmpty() ? null
                        : (int) Math.round(100.0 * actions.stream().filter(RecoveryAction::isCompleted).count() / actions.size()))
                .build());
        return items;
    }

    /**
     * Stability as the inverse of how much mood swings: a flat series scores 100, a series
     * swinging across the full range scores near 0. Needs at least three points to mean anything.
     */
    private Integer moodStability(List<MoodEntry> moods) {
        List<Integer> recent = moodsWithin(moods, RECENT_WINDOW_DAYS).stream()
                .map(MoodEntry::getMoodScore)
                .toList();
        if (recent.size() < MIN_ENTRIES_FOR_STABILITY) {
            return null;
        }
        double mean = recent.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = recent.stream()
                .mapToDouble(score -> Math.pow(score - mean, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);
        return (int) Math.round(Math.max(0, Math.min(100, 100 - stdDev * 2)));
    }

    /** Days since the latest assessment, or null if the user has never taken one. */
    private Long daysSinceAssessment(List<AssessmentSubmission> assessments) {
        return assessments.stream()
                .findFirst()
                .map(latest -> ChronoUnit.DAYS.between(latest.getSubmittedAt(), Instant.now()))
                .orElse(null);
    }

    // --- Helpers ----------------------------------------------------------------------

    private List<MoodEntry> moodsWithin(List<MoodEntry> moods, int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return moods.stream().filter(mood -> mood.getRecordedAt().isAfter(cutoff)).toList();
    }

    private OptionalDouble averageBetween(List<MoodEntry> moods, LocalDate start, LocalDate end, ZoneId zone) {
        return moods.stream()
                .filter(mood -> {
                    LocalDate day = mood.getRecordedAt().atZone(zone).toLocalDate();
                    return !day.isBefore(start) && !day.isAfter(end);
                })
                .mapToInt(MoodEntry::getMoodScore)
                .average();
    }

    private long distinctJournalDays(List<JournalEntry> journals, int days, ZoneId zone) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return journals.stream()
                .filter(entry -> entry.getCreatedAt() != null && entry.getCreatedAt().isAfter(cutoff))
                .map(entry -> entry.getCreatedAt().atZone(zone).toLocalDate())
                .distinct()
                .count();
    }

    /** Days since the user's first piece of data — drives "building your baseline" messaging. */
    private long daysOfData(List<JournalEntry> journals, List<MoodEntry> moods) {
        Instant earliest = journals.stream()
                .map(JournalEntry::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        Instant earliestMood = moods.stream()
                .map(MoodEntry::getRecordedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (earliest == null) {
            earliest = earliestMood;
        } else if (earliestMood != null && earliestMood.isBefore(earliest)) {
            earliest = earliestMood;
        }
        if (earliest == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(earliest, Instant.now()) + 1;
    }
}
