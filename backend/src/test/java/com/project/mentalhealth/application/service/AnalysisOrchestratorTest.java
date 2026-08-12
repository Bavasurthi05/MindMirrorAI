package com.project.mentalhealth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.model.EntrySource;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalysisOrchestratorTest {

    @Mock
    private MlAnalysisPort mlAnalysisPort;

    @Mock
    private AnalysisResultRepository analysisRepository;

    @Mock
    private MoodEntryRepository moodRepository;

    @Mock
    private TriggerEntryRepository triggerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AnalysisOrchestrator orchestrator;
    private User user;

    @BeforeEach
    void setUp() {
        orchestrator = new AnalysisOrchestrator(
                mlAnalysisPort, analysisRepository, moodRepository, triggerRepository,
                new ObjectMapper(), eventPublisher, true, true);
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
    }

    private MlAnalysisPort.JournalAnalysis analysis(double sentimentScore, String emotion,
                                                    List<MlAnalysisPort.DetectedTrigger> triggers) {
        return new MlAnalysisPort.JournalAnalysis(
                sentimentScore >= 0 ? "positive" : "negative",
                sentimentScore,
                emotion,
                Map.of(emotion, 1.0),
                List.of(new MlAnalysisPort.TokenContribution("tired", -1.0)),
                "stress",
                0.82,
                Map.of("stress", 0.82),
                List.of(new MlAnalysisPort.FeatureReason("tired", -1.0, 100.0)),
                "random_forest",
                triggers,
                "random_forest-20260101T000000Z");
    }

    private void echoSave() {
        given(analysisRepository.save(any(AnalysisResult.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submitAndWaitPersistsTheAnalysis() {
        echoSave();
        given(mlAnalysisPort.analyzeJournal(anyString()))
                .willReturn(analysis(-0.4, "fatigue", List.of()));

        AnalysisOrchestrator.CompletedAnalysis completed = orchestrator.submitAndWait(
                user, AnalysisSourceType.JOURNAL, 5L, "I am exhausted");

        assertThat(completed.isOk()).isTrue();
        AnalysisResult stored = completed.result();
        assertThat(stored.getStatus()).isEqualTo(AnalysisStatus.OK);
        assertThat(stored.getPrediction()).isEqualTo("stress");
        assertThat(stored.getModelVersion()).isEqualTo("random_forest-20260101T000000Z");
        assertThat(stored.getAnalyzedAt()).isNotNull();
        assertThat(stored.getAttemptCount()).isEqualTo(1);
        assertThat(stored.getEmotionScores()).contains("fatigue");
    }

    @Test
    void derivesAMoodEntryFromSentiment() {
        echoSave();
        given(mlAnalysisPort.analyzeJournal(anyString()))
                .willReturn(analysis(0.5, "joy", List.of()));
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of());

        orchestrator.submitAndWait(user, AnalysisSourceType.JOURNAL, 5L, "A good day");

        ArgumentCaptor<MoodEntry> captor = ArgumentCaptor.forClass(MoodEntry.class);
        verify(moodRepository).save(captor.capture());
        MoodEntry derived = captor.getValue();
        assertThat(derived.getSource()).isEqualTo(EntrySource.DERIVED);
        assertThat(derived.getMoodLabel()).isEqualTo("joy");
        assertThat(derived.getMoodScore()).isEqualTo(75); // (0.5 + 1) / 2 * 100
    }

    @Test
    void derivesTriggerEntriesAboveTheIntensityFloor() {
        echoSave();
        given(mlAnalysisPort.analyzeJournal(anyString())).willReturn(analysis(-0.3, "fatigue", List.of(
                new MlAnalysisPort.DetectedTrigger("Workload", List.of("deadline", "work"), 7),
                new MlAnalysisPort.DetectedTrigger("Sleep", List.of("tired"), 2))));
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of());

        orchestrator.submitAndWait(user, AnalysisSourceType.JOURNAL, 5L, "Deadline kept me up");

        ArgumentCaptor<TriggerEntry> captor = ArgumentCaptor.forClass(TriggerEntry.class);
        verify(triggerRepository).save(captor.capture());
        TriggerEntry derived = captor.getValue();
        assertThat(derived.getCategory()).isEqualTo("Workload");
        assertThat(derived.getIntensity()).isEqualTo(7);
        assertThat(derived.getSource()).isEqualTo(EntrySource.DERIVED);
    }

    @Test
    void doesNotOverrideASelfReportedMoodForTheSameDay() {
        echoSave();
        given(mlAnalysisPort.analyzeJournal(anyString())).willReturn(analysis(0.2, "calm", List.of()));

        MoodEntry selfReported = new MoodEntry();
        selfReported.setMoodScore(90);
        selfReported.setRecordedAt(Instant.now());
        selfReported.setSource(EntrySource.SELF_REPORTED);
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of(selfReported));

        orchestrator.submitAndWait(user, AnalysisSourceType.JOURNAL, 5L, "Steady day");

        verify(moodRepository, never()).save(any(MoodEntry.class));
    }

    @Test
    void recordsFailureInsteadOfThrowingWhenMlIsDown() {
        echoSave();
        given(mlAnalysisPort.analyzeJournal(anyString()))
                .willThrow(new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE));

        AnalysisOrchestrator.CompletedAnalysis completed = orchestrator.submitAndWait(
                user, AnalysisSourceType.JOURNAL, 5L, "Anything");

        assertThat(completed.isOk()).isFalse();
        assertThat(completed.result().getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(completed.result().getErrorMessage()).contains("unavailable");
        verify(moodRepository, never()).save(any(MoodEntry.class));
    }

    @Test
    void submitQueuesWorkWithoutCallingTheMlService() {
        echoSave();

        orchestrator.submit(user, AnalysisSourceType.JOURNAL, 5L, "Queued text");

        verify(eventPublisher).publishEvent(any(AnalysisRequestedEvent.class));
        verify(mlAnalysisPort, never()).analyzeJournal(anyString());
    }

    @Test
    void mapsSentimentOntoTheMoodScale() {
        assertThat(AnalysisOrchestrator.toMoodScore(-1.0)).isZero();
        assertThat(AnalysisOrchestrator.toMoodScore(0.0)).isEqualTo(50);
        assertThat(AnalysisOrchestrator.toMoodScore(1.0)).isEqualTo(100);
        assertThat(AnalysisOrchestrator.toMoodScore(5.0)).isEqualTo(100); // clamped
    }
}
