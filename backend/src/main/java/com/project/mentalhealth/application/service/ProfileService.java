package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.ProfileUseCase;
import com.project.mentalhealth.application.ports.out.PasswordEncoderPort;
import com.project.mentalhealth.domain.model.JournalEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.model.WellnessGoal;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.domain.repository.WellnessGoalRepository;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.ChangePasswordRequest;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.ProfileResponse;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.UpdateProfileRequest;
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
    private final PasswordEncoderPort passwordEncoder;

    public ProfileService(UserRepository userRepository,
                          JournalEntryRepository journalRepository,
                          MoodEntryRepository moodRepository,
                          WellnessGoalRepository goalRepository,
                          PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.journalRepository = journalRepository;
        this.moodRepository = moodRepository;
        this.goalRepository = goalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse profile(String userEmail) {
        User user = findUser(userEmail);
        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(String userEmail, UpdateProfileRequest request) {
        User user = findUser(userEmail);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        userRepository.save(user);
        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        User user = findUser(userEmail);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }

    private ProfileResponse buildProfileResponse(User user) {
        Long userId = user.getId();

        List<JournalEntry> journals = journalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int streak = computeStreak(journals);

        List<WellnessGoal> goals = goalRepository.findByUserIdOrderByIdAsc(userId);
        long goalsCompleted = goals.stream().filter(WellnessGoal::isCompleted).count();

        return ProfileResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
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
