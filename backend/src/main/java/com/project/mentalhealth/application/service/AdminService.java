package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.AdminUseCase;
import com.project.mentalhealth.application.ports.in.FeedbackUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminUserResponse;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService implements AdminUseCase {

    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final JournalEntryRepository journalRepository;
    private final MoodEntryRepository moodRepository;
    private final AssessmentSubmissionRepository assessmentRepository;
    private final TriggerEntryRepository triggerRepository;
    private final RecoveryActionRepository recoveryRepository;
    private final FeedbackUseCase feedbackUseCase;
    private final MlAnalysisPort mlAnalysisPort;

    public AdminService(UserRepository userRepository,
                        JournalEntryRepository journalRepository,
                        MoodEntryRepository moodRepository,
                        AssessmentSubmissionRepository assessmentRepository,
                        TriggerEntryRepository triggerRepository,
                        RecoveryActionRepository recoveryRepository,
                        FeedbackUseCase feedbackUseCase,
                        MlAnalysisPort mlAnalysisPort) {
        this.userRepository = userRepository;
        this.journalRepository = journalRepository;
        this.moodRepository = moodRepository;
        this.assessmentRepository = assessmentRepository;
        this.triggerRepository = triggerRepository;
        this.recoveryRepository = recoveryRepository;
        this.feedbackUseCase = feedbackUseCase;
        this.mlAnalysisPort = mlAnalysisPort;
    }

    /** How many months of sign-up history the growth chart shows. */
    private static final int GROWTH_MONTHS = 6;

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewResponse overview() {
        List<User> users = userRepository.findAll();
        return AdminOverviewResponse.builder()
                .totalUsers(userRepository.count())
                .verifiedUsers(userRepository.countByEmailVerifiedTrue())
                .totalJournalEntries(journalRepository.count())
                .totalMoodEntries(moodRepository.count())
                .totalAssessments(assessmentRepository.count())
                .totalTriggers(triggerRepository.count())
                .totalRecoveryActions(recoveryRepository.count())
                .userGrowth(buildUserGrowth(users))
                .triggerDistribution(buildTriggerDistribution())
                .build();
    }

    /**
     * Real sign-ups per month, with a running total.
     *
     * <p>Months with no sign-ups are still included so the chart has an even time axis
     * rather than silently compressing quiet periods.
     */
    private List<AdminOverviewResponse.GrowthPoint> buildUserGrowth(List<User> users) {
        Map<YearMonth, Long> signupsByMonth = users.stream()
                .filter(user -> user.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        user -> YearMonth.from(user.getCreatedAt().atZone(ZoneOffset.UTC)),
                        Collectors.counting()));

        YearMonth start = YearMonth.now(ZoneOffset.UTC).minusMonths(GROWTH_MONTHS - 1L);
        // Everyone who joined before the window still counts toward the running total.
        long cumulative = users.stream()
                .filter(user -> user.getCreatedAt() != null)
                .filter(user -> YearMonth.from(user.getCreatedAt().atZone(ZoneOffset.UTC)).isBefore(start))
                .count();

        List<AdminOverviewResponse.GrowthPoint> points = new ArrayList<>();
        for (int i = 0; i < GROWTH_MONTHS; i++) {
            YearMonth month = start.plusMonths(i);
            long newUsers = signupsByMonth.getOrDefault(month, 0L);
            cumulative += newUsers;
            points.add(AdminOverviewResponse.GrowthPoint.builder()
                    .label(month.format(MONTH_LABEL))
                    .newUsers(newUsers)
                    .cumulativeUsers(cumulative)
                    .build());
        }
        return points;
    }

    private List<AdminOverviewResponse.CategoryCount> buildTriggerDistribution() {
        return triggerRepository.findAll().stream()
                .filter(trigger -> trigger.getConfirmation() != TriggerConfirmation.DISMISSED)
                .collect(Collectors.groupingBy(TriggerEntry::getCategory, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> AdminOverviewResponse.CategoryCount.builder()
                        .category(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(AdminUserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AdminUserResponse setUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        user.setEnabled(enabled);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> listFeedback() {
        return feedbackUseCase.listAll();
    }

    @Override
    public MlAnalysisPort.ModelMetrics modelMetrics() {
        return mlAnalysisPort.modelMetrics();
    }
}
