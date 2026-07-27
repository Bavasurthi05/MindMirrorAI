package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.ProfileUseCase;
import com.project.mentalhealth.domain.model.JournalEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.model.WellnessGoal;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.domain.repository.WellnessGoalRepository;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.ProfileResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TreeSet;

@Service
public class ProfileService implements ProfileUseCase {

    private final UserRepository userRepository;
    private final JournalEntryRepository journalRepository;
    private final MoodEntryRepository moodRepository;
    private final WellnessGoalRepository goalRepository;

    public ProfileService(UserRepository userRepository,
                          JournalEntryRepository journalRepository,
                          MoodEntryRepository moodRepository,
                          WellnessGoalRepository goalRepository) {
        this.userRepository = userRepository;
        this.journalRepository = journalRepository;
        this.moodRepository = moodRepository;
        this.goalRepository = goalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse profile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        Long userId = user.getId();

        List<JournalEntry> journals = journalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int streak = computeStreak(journals);

        List<WellnessGoal> goals = goalRepository.findByUserIdOrderByIdAsc(userId);
        long goalsCompleted = goals.stream().filter(WellnessGoal::isCompleted).count();

        return ProfileResponse.builder()
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() == null ? "ROLE_USER" : user.getRole().getName())
                .emailVerified(user.isEmailVerified())
                .memberSince(user.getCreatedAt())
                .journalStreak(streak)
                .journalCount(journals.size())
                .moodCount(moodRepository.findByUserIdOrderByRecordedAtDesc(userId).size())
                .goalsCompleted(goalsCompleted)
                .goalsTotal(goals.size())
                .build();
    }

    /** Consecutive-day journaling streak ending today or yesterday. */
    private int computeStreak(List<JournalEntry> journals) {
        if (journals.isEmpty()) {
            return 0;
        }
        TreeSet<LocalDate> days = new TreeSet<>();
        for (JournalEntry entry : journals) {
            days.add(entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate());
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate cursor = days.last();
        if (cursor.isBefore(today.minusDays(1))) {
            return 0; // streak broken (no entry today or yesterday)
        }

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
