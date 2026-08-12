package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.EntrySource;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.checkin.dto.CheckInRequest;
import com.project.mentalhealth.interfaces.api.v1.checkin.dto.CheckInStatusResponse;
import com.project.mentalhealth.interfaces.api.v1.mood.dto.MoodEntryResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * The daily check-in: one tap for mood, optionally a line of text.
 *
 * <p>This is the lowest-friction way to gather the data every chart depends on. A check-in is
 * always {@code SELF_REPORTED}, which outranks anything derived from journal analysis for the
 * same day. Days are bucketed in the user's own timezone, not UTC.
 */
@Service
public class CheckInService {

    private final MoodEntryRepository moodRepository;
    private final UserRepository userRepository;
    private final UserPreferencesService preferencesService;
    private final AnalysisOrchestrator analysisOrchestrator;

    public CheckInService(MoodEntryRepository moodRepository,
                          UserRepository userRepository,
                          UserPreferencesService preferencesService,
                          AnalysisOrchestrator analysisOrchestrator) {
        this.moodRepository = moodRepository;
        this.userRepository = userRepository;
        this.preferencesService = preferencesService;
        this.analysisOrchestrator = analysisOrchestrator;
    }

    @Transactional
    public MoodEntryResponse submit(String userEmail, CheckInRequest request) {
        User user = requireUser(userEmail);
        ZoneId zone = preferencesService.zoneFor(user.getId());

        MoodEntry existing = todaysSelfReported(user.getId(), zone);
        MoodEntry entry = existing != null ? existing : new MoodEntry();
        entry.setUser(user);
        entry.setMoodScore(request.getMoodScore());
        entry.setMoodLabel(request.getMoodLabel());
        entry.setNote(request.getNote());
        entry.setRecordedAt(Instant.now());
        entry.setSource(EntrySource.SELF_REPORTED);
        MoodEntry saved = moodRepository.save(entry);

        // A written note is text worth analyzing, same as a journal entry.
        if (request.getNote() != null && !request.getNote().isBlank()) {
            analysisOrchestrator.submit(user, AnalysisSourceType.CHECKIN, saved.getId(), request.getNote());
        }

        return MoodEntryResponse.from(saved);
    }

    /** Whether the user has already checked in today, so the UI can show the card at most once a day. */
    @Transactional(readOnly = true)
    public CheckInStatusResponse status(String userEmail) {
        User user = requireUser(userEmail);
        ZoneId zone = preferencesService.zoneFor(user.getId());
        MoodEntry today = todaysSelfReported(user.getId(), zone);
        return CheckInStatusResponse.builder()
                .checkedInToday(today != null)
                .localDate(LocalDate.now(zone).toString())
                .timezone(zone.getId())
                .lastCheckInAt(today == null ? null : today.getRecordedAt())
                .lastMoodScore(today == null ? null : today.getMoodScore())
                .build();
    }

    /** Re-checking in on the same day updates that day's entry rather than stacking a second one. */
    private MoodEntry todaysSelfReported(Long userId, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        Instant since = today.minusDays(1).atStartOfDay(zone).toInstant();
        List<MoodEntry> recent = moodRepository
                .findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(userId, since);
        return recent.stream()
                .filter(entry -> EntrySource.SELF_REPORTED.equals(entry.getSource()))
                .filter(entry -> entry.getRecordedAt().atZone(zone).toLocalDate().equals(today))
                .findFirst()
                .orElse(null);
    }

    /** Consecutive days ending today (or yesterday) with a self-reported check-in. */
    @Transactional(readOnly = true)
    public int checkInStreak(Long userId, ZoneId zone) {
        List<MoodEntry> entries = moodRepository.findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(
                userId, Instant.now().minus(400, ChronoUnit.DAYS));
        List<LocalDate> days = entries.stream()
                .filter(entry -> EntrySource.SELF_REPORTED.equals(entry.getSource()))
                .map(entry -> entry.getRecordedAt().atZone(zone).toLocalDate())
                .distinct()
                .sorted(java.util.Comparator.reverseOrder())
                .toList();
        if (days.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now(zone);
        LocalDate cursor = days.get(0);
        // A streak stays alive if the most recent check-in was today or yesterday.
        if (cursor.isBefore(today.minusDays(1))) {
            return 0;
        }

        int streak = 1;
        for (int i = 1; i < days.size(); i++) {
            if (days.get(i).equals(cursor.minusDays(1))) {
                streak++;
                cursor = days.get(i);
            } else {
                break;
            }
        }
        return streak;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
