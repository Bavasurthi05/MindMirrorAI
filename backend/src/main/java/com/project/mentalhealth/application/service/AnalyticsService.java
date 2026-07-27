package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.AnalyticsUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.AssessmentSubmission;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.RecoveryAction;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.AnalyticsOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.AnalyticsOverviewResponse.EmotionPoint;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.AnalyticsOverviewResponse.HeatCell;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.AnalyticsOverviewResponse.Metric;
import com.project.mentalhealth.interfaces.api.v1.analytics.dto.AnalyticsOverviewResponse.SeriesPoint;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService implements AnalyticsUseCase {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int HEATMAP_DAYS = 35;
    private static final int MOOD_SERIES_DAYS = 14;
    private static final int TIMELINE_POINTS = 8;

    private final UserRepository userRepository;
    private final MoodEntryRepository moodRepository;
    private final TriggerEntryRepository triggerRepository;
    private final AssessmentSubmissionRepository assessmentRepository;
    private final RecoveryActionRepository recoveryRepository;
    private final JournalEntryRepository journalRepository;
    private final MlAnalysisPort mlAnalysisPort;

    public AnalyticsService(UserRepository userRepository,
                            MoodEntryRepository moodRepository,
                            TriggerEntryRepository triggerRepository,
                            AssessmentSubmissionRepository assessmentRepository,
                            RecoveryActionRepository recoveryRepository,
                            JournalEntryRepository journalRepository,
                            MlAnalysisPort mlAnalysisPort) {
        this.userRepository = userRepository;
        this.moodRepository = moodRepository;
        this.triggerRepository = triggerRepository;
        this.assessmentRepository = assessmentRepository;
        this.recoveryRepository = recoveryRepository;
        this.journalRepository = journalRepository;
        this.mlAnalysisPort = mlAnalysisPort;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse overview(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        Long userId = user.getId();

        List<MoodEntry> moods = moodRepository.findByUserIdOrderByRecordedAtDesc(userId);
        List<TriggerEntry> triggers = triggerRepository.findByUserIdOrderByOccurredAtDesc(userId);
        List<AssessmentSubmission> assessments = assessmentRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        List<RecoveryAction> recoveryActions = recoveryRepository.findByUserIdOrderByIdAsc(userId);

        double averageMood = moods.stream().mapToInt(MoodEntry::getMoodScore).average().orElse(0);

        // Average trigger intensity per category (0-10 scale in storage).
        Map<String, Double> triggerByCategory = triggers.stream()
                .collect(Collectors.groupingBy(TriggerEntry::getCategory,
                        Collectors.averagingInt(TriggerEntry::getIntensity)));
        double avgTriggerIntensity = triggers.stream().mapToInt(TriggerEntry::getIntensity).average().orElse(0);

        long completedActions = recoveryActions.stream().filter(RecoveryAction::isCompleted).count();
        int recoveryScore = recoveryActions.isEmpty() ? 0
                : (int) Math.round(100.0 * completedActions / recoveryActions.size());

        int overallWellness = computeOverallWellness(assessments, averageMood);

        return AnalyticsOverviewResponse.builder()
                .overallWellness(overallWellness)
                .radar(buildRadar(averageMood, avgTriggerIntensity, triggerByCategory, recoveryScore))
                .heatmap(buildHeatmap(moods))
                .emotionTimeline(buildEmotionTimeline(moods))
                .weeklyTrend(buildWeeklyTrend(moods))
                .moodSeries(buildMoodSeries(moods))
                .emotionDistribution(buildEmotionDistribution(moods))
                .triggerDistribution(buildTriggerDistribution(triggerByCategory))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MlAnalysisPort.WeeklyInsights weeklyInsights(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        Long userId = user.getId();

        List<MoodEntry> moods = moodRepository.findByUserIdOrderByRecordedAtDesc(userId);
        List<Integer> scores = new ArrayList<>(moods.stream().map(MoodEntry::getMoodScore).toList());
        Collections.reverse(scores);

        List<TriggerEntry> triggers = triggerRepository.findByUserIdOrderByOccurredAtDesc(userId);
        double avgTriggerIntensity = triggers.stream().mapToInt(TriggerEntry::getIntensity).average().orElse(0);
        int journalCount = (int) journalRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1))
                .getTotalElements();

        return mlAnalysisPort.weeklyInsights(scores, journalCount, triggers.size(), avgTriggerIntensity);
    }

    private int computeOverallWellness(List<AssessmentSubmission> assessments, double averageMood) {
        AssessmentSubmission latest = assessments.stream().findFirst().orElse(null);
        if (latest != null && latest.getMaxScore() > 0) {
            return (int) Math.round(100.0 * latest.getTotalScore() / latest.getMaxScore());
        }
        return (int) Math.round(averageMood);
    }

    private List<Metric> buildRadar(double averageMood,
                                    double avgTriggerIntensity,
                                    Map<String, Double> triggerByCategory,
                                    int recoveryScore) {
        int happiness = clamp((int) Math.round(averageMood));
        int stress = clamp((int) Math.round(avgTriggerIntensity * 10));
        int sleep = clamp(100 - (int) Math.round(categoryIntensity(triggerByCategory, "Sleep") * 10));
        int social = clamp(100 - (int) Math.round(categoryIntensity(triggerByCategory, "Social") * 10));
        int confidence = clamp((int) Math.round(0.5 * averageMood + 0.5 * (100 - stress)));
        int motivation = clamp((int) Math.round(0.5 * recoveryScore + 0.5 * averageMood));

        List<Metric> radar = new ArrayList<>();
        radar.add(Metric.builder().label("Stress").value(stress).build());
        radar.add(Metric.builder().label("Confidence").value(confidence).build());
        radar.add(Metric.builder().label("Sleep").value(sleep).build());
        radar.add(Metric.builder().label("Motivation").value(motivation).build());
        radar.add(Metric.builder().label("Social").value(social).build());
        radar.add(Metric.builder().label("Happiness").value(happiness).build());
        return radar;
    }

    private double categoryIntensity(Map<String, Double> triggerByCategory, String category) {
        return triggerByCategory.getOrDefault(category, 0.0);
    }

    private List<HeatCell> buildHeatmap(List<MoodEntry> moods) {
        Map<LocalDate, List<Integer>> byDay = new LinkedHashMap<>();
        for (MoodEntry mood : moods) {
            LocalDate day = mood.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate();
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(mood.getMoodScore());
        }

        List<HeatCell> cells = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = HEATMAP_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<Integer> scores = byDay.get(day);
            Integer avg = (scores == null || scores.isEmpty()) ? null
                    : (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
            cells.add(HeatCell.builder().date(day.format(ISO_DATE)).score(avg).build());
        }
        return cells;
    }

    private List<EmotionPoint> buildEmotionTimeline(List<MoodEntry> moods) {
        return moods.stream()
                .limit(TIMELINE_POINTS)
                .map(mood -> EmotionPoint.builder()
                        .date(mood.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate().format(ISO_DATE))
                        .label(mood.getMoodLabel() == null ? "neutral" : mood.getMoodLabel())
                        .score(mood.getMoodScore())
                        .build())
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.reverse(list); // chronological order
                    return list;
                }));
    }

    private List<Metric> buildWeeklyTrend(List<MoodEntry> moods) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Metric> trend = new ArrayList<>();
        for (int week = 3; week >= 0; week--) {
            LocalDate start = today.minusDays((week + 1L) * 7 - 1);
            LocalDate end = today.minusDays(week * 7L);
            double avg = moods.stream()
                    .filter(mood -> {
                        LocalDate day = mood.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate();
                        return !day.isBefore(start) && !day.isAfter(end);
                    })
                    .mapToInt(MoodEntry::getMoodScore)
                    .average()
                    .orElse(0);
            trend.add(Metric.builder().label("Week " + (4 - week)).value(Math.round(avg)).build());
        }
        return trend;
    }

    private List<SeriesPoint> buildMoodSeries(List<MoodEntry> moods) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate cutoff = today.minusDays(MOOD_SERIES_DAYS - 1L);
        return moods.stream()
                .filter(mood -> !mood.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate().isBefore(cutoff))
                .sorted((a, b) -> a.getRecordedAt().compareTo(b.getRecordedAt()))
                .map(mood -> SeriesPoint.builder()
                        .date(mood.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate().format(ISO_DATE))
                        .score(mood.getMoodScore())
                        .build())
                .toList();
    }

    private List<Metric> buildEmotionDistribution(List<MoodEntry> moods) {
        Map<String, Long> counts = moods.stream()
                .collect(Collectors.groupingBy(
                        mood -> mood.getMoodLabel() == null ? "neutral" : mood.getMoodLabel(),
                        Collectors.counting()));
        return counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> Metric.builder().label(e.getKey()).value(e.getValue()).build())
                .toList();
    }

    private List<Metric> buildTriggerDistribution(Map<String, Double> triggerByCategory) {
        return triggerByCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> Metric.builder()
                        .label(e.getKey())
                        .value(Math.round(e.getValue() * 10))
                        .build())
                .toList();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
