package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.EntrySource;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.checkin.dto.CheckInRequest;
import com.project.mentalhealth.interfaces.api.v1.checkin.dto.CheckInStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckInServiceTest {

    @Mock private MoodEntryRepository moodRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserPreferencesService preferencesService;
    @Mock private AnalysisOrchestrator analysisOrchestrator;

    private CheckInService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CheckInService(moodRepository, userRepository, preferencesService, analysisOrchestrator);
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(preferencesService.zoneFor(anyLong())).willReturn(ZoneOffset.UTC);
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of());
        given(moodRepository.save(any(MoodEntry.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private CheckInRequest request(int score, String note) {
        CheckInRequest request = new CheckInRequest();
        request.setMoodScore(score);
        request.setMoodLabel("calm");
        request.setNote(note);
        return request;
    }

    private MoodEntry entry(String source, Instant when) {
        MoodEntry entry = new MoodEntry();
        entry.setMoodScore(70);
        entry.setSource(source);
        entry.setRecordedAt(when);
        return entry;
    }

    @Test
    void checkInIsAlwaysSelfReported() {
        service.submit("user@example.com", request(80, null));

        ArgumentCaptor<MoodEntry> captor = ArgumentCaptor.forClass(MoodEntry.class);
        verify(moodRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(EntrySource.SELF_REPORTED);
        assertThat(captor.getValue().getMoodScore()).isEqualTo(80);
    }

    @Test
    void aNoteIsAnalyzedLikeAnyOtherUserText() {
        service.submit("user@example.com", request(40, "Work is piling up again"));

        verify(analysisOrchestrator).submit(eq(user), eq(AnalysisSourceType.CHECKIN), any(),
                eq("Work is piling up again"));
    }

    @Test
    void anEmptyNoteIsNotSentForAnalysis() {
        service.submit("user@example.com", request(60, "   "));

        verify(analysisOrchestrator, never()).submit(any(), any(), any(), anyString());
    }

    @Test
    void checkingInTwiceInADayUpdatesTheSameEntry() {
        MoodEntry existing = entry(EntrySource.SELF_REPORTED, Instant.now());
        existing.setId(9L);
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of(existing));

        service.submit("user@example.com", request(30, null));

        ArgumentCaptor<MoodEntry> captor = ArgumentCaptor.forClass(MoodEntry.class);
        verify(moodRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(9L);
        assertThat(captor.getValue().getMoodScore()).isEqualTo(30);
    }

    @Test
    void aDerivedEntryDoesNotCountAsCheckingIn() {
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of(entry(EntrySource.DERIVED, Instant.now())));

        CheckInStatusResponse status = service.status("user@example.com");

        assertThat(status.isCheckedInToday()).isFalse();
    }

    @Test
    void statusReportsTodayInTheUsersOwnTimezone() {
        ZoneId sydney = ZoneId.of("Australia/Sydney");
        given(preferencesService.zoneFor(anyLong())).willReturn(sydney);

        CheckInStatusResponse status = service.status("user@example.com");

        assertThat(status.getTimezone()).isEqualTo("Australia/Sydney");
        assertThat(status.getLocalDate()).isEqualTo(LocalDate.now(sydney).toString());
    }

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        Instant now = Instant.now();
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of(
                        entry(EntrySource.SELF_REPORTED, now),
                        entry(EntrySource.SELF_REPORTED, now.minusSeconds(86_400)),
                        entry(EntrySource.SELF_REPORTED, now.minusSeconds(2 * 86_400))));

        assertThat(service.checkInStreak(1L, ZoneOffset.UTC)).isEqualTo(3);
    }

    @Test
    void streakBreaksOnAMissedDay() {
        Instant now = Instant.now();
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of(
                        entry(EntrySource.SELF_REPORTED, now),
                        entry(EntrySource.SELF_REPORTED, now.minusSeconds(3 * 86_400))));

        assertThat(service.checkInStreak(1L, ZoneOffset.UTC)).isEqualTo(1);
    }

    @Test
    void aStaleStreakIsZeroRatherThanStillCounting() {
        given(moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(anyLong(), any()))
                .willReturn(List.of(entry(EntrySource.SELF_REPORTED, Instant.now().minusSeconds(5 * 86_400))));

        assertThat(service.checkInStreak(1L, ZoneOffset.UTC)).isZero();
    }
}
