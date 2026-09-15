package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.model.JournalEntry;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.RecoveryAction;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.project.mentalhealth.domain.model.UserPreferences;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MoodEntryRepository moodRepository;
    @Mock private TriggerEntryRepository triggerRepository;
    @Mock private AssessmentSubmissionRepository assessmentRepository;
    @Mock private RecoveryActionRepository recoveryRepository;
    @Mock private JournalEntryRepository journalRepository;
    @Mock private AnalysisResultRepository analysisRepository;
    @Mock private UserPreferencesService preferencesService;
    @Mock private CheckInService checkInService;
    @Mock private MindsetScoringService mindsetScoringService;

    private DashboardService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new DashboardService(userRepository, moodRepository, triggerRepository,
                assessmentRepository, recoveryRepository, journalRepository, analysisRepository,
                preferencesService, checkInService, mindsetScoringService);
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        emptyRepositories();
    }

    private void emptyRepositories() {
        given(moodRepository.findByUserIdOrderByRecordedAtDesc(anyLong())).willReturn(List.of());
        given(triggerRepository.findByUserIdAndConfirmationNotOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of());
        given(triggerRepository.findByUserIdAndConfirmationOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of());
        given(assessmentRepository.findByUserIdOrderBySubmittedAtDesc(anyLong())).willReturn(List.of());
        given(recoveryRepository.findByUserIdOrderByIdAsc(anyLong())).willReturn(List.of());
        given(journalRepository.findByUserIdOrderByCreatedAtDesc(anyLong())).willReturn(List.of());
        given(analysisRepository.findByUserIdAndStatusAndSourceTypeNotOrderByAnalyzedAtDesc(anyLong(), any(), any(), any()))
                .willReturn(List.of());
        given(analysisRepository.countByUserIdAndStatusAndSourceTypeNot(anyLong(), any(), any())).willReturn(0L);
        given(preferencesService.zoneFor(anyLong())).willReturn(ZoneOffset.UTC);
        given(preferencesService.preferencesOrDefaults(any())).willReturn(new UserPreferences());
        given(checkInService.checkInStreak(anyLong(), any())).willReturn(0);
        // The composite score has its own tests; here we only care that the dashboard uses it.
        given(mindsetScoringService.mindsetFor(any())).willReturn(MindsetResponse.builder().build());
    }

    private void withMindsetScore(Integer score) {
        given(mindsetScoringService.mindsetFor(any()))
                .willReturn(MindsetResponse.builder().score(score).band("Steady").build());
    }

    private MoodEntry mood(int score, int daysAgo) {
        MoodEntry entry = new MoodEntry();
        entry.setMoodScore(score);
        entry.setMoodLabel("calm");
        entry.setRecordedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        return entry;
    }

    private JournalEntry journal(int daysAgo) {
        JournalEntry entry = new JournalEntry();
        entry.setTitle("Entry");
        entry.setContent("Content");
        entry.setCreatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        return entry;
    }

    @Test
    void newAccountGetsNullsRatherThanInventedNumbers() {
        DashboardResponse response = service.dashboard("user@example.com");

        assertThat(response.getWellnessScore()).isNull();
        assertThat(response.getWellnessDelta()).isNull();
        assertThat(response.getWellnessUpdatedAt()).isNull();
        assertThat(response.getSummary().getStress()).isNull();
        assertThat(response.getSummary().getEnergy()).isNull();
        assertThat(response.getSummary().getReflection()).isNull();
        assertThat(response.getRecentAnalyses()).isEmpty();
        assertThat(response.getTriggerSummary()).isEmpty();
        assertThat(response.getRecommendations()).isEmpty();
        assertThat(response.getProgressItems())
                .allSatisfy(item -> assertThat(item.getProgress()).isNull());
        assertThat(response.getDataCompleteness().isHasJournal()).isFalse();
        assertThat(response.getDataCompleteness().getDaysOfData()).isZero();
    }

    @Test
    void moodWeekAlwaysHasSevenDaysAndNullsWhereThereIsNoEntry() {
        given(moodRepository.findByUserIdOrderByRecordedAtDesc(anyLong()))
                .willReturn(List.of(mood(80, 0)));

        List<DashboardResponse.MoodDay> week = service.dashboard("user@example.com").getMoodWeek();

        assertThat(week).hasSize(7);
        assertThat(week.get(6).getScore()).isEqualTo(80);
        assertThat(week.subList(0, 6)).allSatisfy(day -> assertThat(day.getScore()).isNull());
    }

    @Test
    void wellnessComesFromTheSharedCompositeScore() {
        withMindsetScore(70);
        given(moodRepository.findByUserIdOrderByRecordedAtDesc(anyLong()))
                .willReturn(List.of(mood(80, 0), mood(60, 1)));

        DashboardResponse response = service.dashboard("user@example.com");

        // Dashboard, mirror and reports must all show the same number.
        assertThat(response.getWellnessScore()).isEqualTo(70);
        assertThat(response.getMindsetBand()).isEqualTo("Steady");
        assertThat(response.getWellnessUpdatedAt()).isNotNull();
    }

    @Test
    void deltaComparesThisWeekAgainstLastWeek() {
        given(moodRepository.findByUserIdOrderByRecordedAtDesc(anyLong()))
                .willReturn(List.of(mood(80, 1), mood(60, 8)));

        assertThat(service.dashboard("user@example.com").getWellnessDelta()).isEqualTo(20);
    }

    @Test
    void deltaIsNullWhenTheEarlierWindowIsEmpty() {
        given(moodRepository.findByUserIdOrderByRecordedAtDesc(anyLong()))
                .willReturn(List.of(mood(80, 1)));

        assertThat(service.dashboard("user@example.com").getWellnessDelta()).isNull();
    }

    @Test
    void moodStabilityNeedsAtLeastThreeEntries() {
        given(moodRepository.findByUserIdOrderByRecordedAtDesc(anyLong()))
                .willReturn(List.of(mood(70, 0), mood(72, 1)));

        DashboardResponse.Progress stability = service.dashboard("user@example.com")
                .getProgressItems().get(0);
        assertThat(stability.getLabel()).isEqualTo("Mood stability");
        assertThat(stability.getProgress()).isNull();
    }

    @Test
    void steadyMoodScoresHigherStabilityThanASwingingOne() {
        given(moodRepository.findByUserIdOrderByRecordedAtDesc(anyLong()))
                .willReturn(List.of(mood(70, 0), mood(71, 1), mood(69, 2)));
        Integer steady = service.dashboard("user@example.com").getProgressItems().get(0).getProgress();

        given(moodRepository.findByUserIdOrderByRecordedAtDesc(anyLong()))
                .willReturn(List.of(mood(10, 0), mood(95, 1), mood(30, 2)));
        Integer swinging = service.dashboard("user@example.com").getProgressItems().get(0).getProgress();

        assertThat(steady).isGreaterThan(swinging);
    }

    @Test
    void triggerSummaryBandsTheTopCategories() {
        TriggerEntry workload = new TriggerEntry();
        workload.setCategory("Workload");
        workload.setIntensity(8);
        workload.setOccurredAt(Instant.now());
        given(triggerRepository.findByUserIdAndConfirmationNotOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of(workload));

        DashboardResponse response = service.dashboard("user@example.com");

        assertThat(response.getTriggerSummary()).hasSize(1);
        assertThat(response.getTriggerSummary().get(0).getLabel()).isEqualTo("Workload");
        assertThat(response.getTriggerSummary().get(0).getValue()).isEqualTo("High");
        assertThat(response.getSummary().getStress()).isEqualTo("High");
    }

    @Test
    void recommendationsComeFromIncompleteRecoveryActions() {
        RecoveryAction done = new RecoveryAction();
        done.setTitle("Meditation");
        done.setCompleted(true);
        RecoveryAction todo = new RecoveryAction();
        todo.setTitle("Walking");
        todo.setDescription("A gentle walk");
        todo.setCompleted(false);
        given(recoveryRepository.findByUserIdOrderByIdAsc(anyLong())).willReturn(List.of(done, todo));

        DashboardResponse response = service.dashboard("user@example.com");

        assertThat(response.getRecommendations()).hasSize(1);
        assertThat(response.getRecommendations().get(0).getTitle()).isEqualTo("Walking");
        assertThat(response.getProgressItems().get(2).getProgress()).isEqualTo(50);
    }

    @Test
    void headlineReflectsActualJournalingThisWeek() {
        given(journalRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                .willReturn(List.of(journal(1), journal(2)));

        DashboardResponse response = service.dashboard("user@example.com");

        assertThat(response.getHeadline()).contains("2 reflections");
        assertThat(response.getSummary().getReflection()).isEqualTo("Building");
        assertThat(response.getDataCompleteness().isHasJournal()).isTrue();
    }

    @Test
    void pendingAnalysesAreReportedForTheAnalyzingState() {
        given(analysisRepository.countByUserIdAndStatusAndSourceTypeNot(1L, AnalysisStatus.PENDING, AnalysisSourceType.SOCIAL))
                .willReturn(2L);

        assertThat(service.dashboard("user@example.com").getDataCompleteness().getPendingAnalyses())
                .isEqualTo(2);
    }
}
