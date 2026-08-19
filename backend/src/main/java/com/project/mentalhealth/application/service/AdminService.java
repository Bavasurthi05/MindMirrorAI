package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.AdminUseCase;
import com.project.mentalhealth.application.ports.in.FeedbackUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.AdminAuditLog;
import com.project.mentalhealth.domain.model.Role;
import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AdminAuditLogRepository;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.RoleRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminAuditEntryResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminOverviewResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminUserResponse;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService implements AdminUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

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
    private final RoleRepository roleRepository;
    private final AdminAuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository,
                        JournalEntryRepository journalRepository,
                        MoodEntryRepository moodRepository,
                        AssessmentSubmissionRepository assessmentRepository,
                        TriggerEntryRepository triggerRepository,
                        RecoveryActionRepository recoveryRepository,
                        FeedbackUseCase feedbackUseCase,
                        MlAnalysisPort mlAnalysisPort,
                        RoleRepository roleRepository,
                        AdminAuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.journalRepository = journalRepository;
        this.moodRepository = moodRepository;
        this.assessmentRepository = assessmentRepository;
        this.triggerRepository = triggerRepository;
        this.recoveryRepository = recoveryRepository;
        this.feedbackUseCase = feedbackUseCase;
        this.mlAnalysisPort = mlAnalysisPort;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
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

    /** Roles an admin may assign through the UI. */
    public static final Set<String> ASSIGNABLE_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");
    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    @Override
    @Transactional
    public AdminUserResponse setUserEnabled(String actorEmail, Long userId, boolean enabled) {
        User user = requireUser(userId);
        boolean previous = user.isEnabled();

        if (!enabled) {
            // Disabling yourself signs you out of an account you may be the only way back into.
            requireNotSelf(actorEmail, user, "You cannot disable your own account");
            requireNotLastAdmin(user, "Disabling this account would leave no active administrator");
        }

        user.setEnabled(enabled);
        AdminUserResponse response = AdminUserResponse.from(userRepository.save(user));

        audit(AdminAuditLog.ACTION_ACCESS_CHANGED, actorEmail, user,
                previous ? "enabled" : "disabled", enabled ? "enabled" : "disabled", null);
        log.info("{} {} account {}", actorEmail, enabled ? "enabled" : "disabled", user.getEmail());
        return response;
    }

    /**
     * Change a user's role.
     *
     * <p>Two refusals matter more than the happy path: an admin cannot change their own role,
     * and the last active admin cannot be demoted. Either would leave the deployment with no
     * way into the admin area short of editing the database by hand.
     */
    @Override
    @Transactional
    public AdminUserResponse setUserRole(String actorEmail, Long userId, String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
        if (!ASSIGNABLE_ROLES.contains(normalized)) {
            throw new ApiException("Unknown role: " + roleName, HttpStatus.BAD_REQUEST);
        }

        User user = requireUser(userId);
        String previous = user.getRole() == null ? null : user.getRole().getName();
        if (normalized.equals(previous)) {
            return AdminUserResponse.from(user);
        }

        // Changing your own role is never necessary and is the easiest way to lock yourself out.
        requireNotSelf(actorEmail, user, "You cannot change your own role");
        if (ADMIN_ROLE.equals(previous)) {
            requireNotLastAdmin(user, "This is the last active administrator");
        }

        Role role = roleRepository.findByName(normalized)
                .orElseThrow(() -> new ApiException("Role " + normalized + " is not configured",
                        HttpStatus.INTERNAL_SERVER_ERROR));
        user.setRole(role);
        AdminUserResponse response = AdminUserResponse.from(userRepository.save(user));

        audit(AdminAuditLog.ACTION_ROLE_CHANGED, actorEmail, user, previous, normalized, null);
        log.warn("{} changed the role of {} from {} to {}", actorEmail, user.getEmail(), previous, normalized);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAuditEntryResponse> auditLog(int limit) {
        return auditLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(AdminAuditEntryResponse::from)
                .toList();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private void requireNotSelf(String actorEmail, User target, String message) {
        if (target.getEmail() != null && target.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new ApiException(message, HttpStatus.CONFLICT);
        }
    }

    /** Refuses a change that would remove the only administrator who can still sign in. */
    private void requireNotLastAdmin(User target, String message) {
        boolean targetIsActiveAdmin = target.getRole() != null
                && ADMIN_ROLE.equals(target.getRole().getName())
                && target.isEnabled();
        if (targetIsActiveAdmin && userRepository.countByRoleNameAndEnabledTrue(ADMIN_ROLE) <= 1) {
            throw new ApiException(message + ". Promote another administrator first.", HttpStatus.CONFLICT);
        }
    }

    private void audit(String action, String actorEmail, User target,
                       String previousValue, String newValue, String detail) {
        AdminAuditLog entry = new AdminAuditLog();
        entry.setAction(action);
        entry.setActorEmail(actorEmail);
        entry.setTargetUserId(target.getId());
        entry.setTargetEmail(target.getEmail());
        entry.setPreviousValue(previousValue);
        entry.setNewValue(newValue);
        entry.setDetail(detail);
        auditLogRepository.save(entry);
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
