package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.AdminUseCase;
import com.project.mentalhealth.application.ports.in.FeedbackUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
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

import java.util.Comparator;
import java.util.List;

@Service
public class AdminService implements AdminUseCase {

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

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewResponse overview() {
        return AdminOverviewResponse.builder()
                .totalUsers(userRepository.count())
                .verifiedUsers(userRepository.countByEmailVerifiedTrue())
                .totalJournalEntries(journalRepository.count())
                .totalMoodEntries(moodRepository.count())
                .totalAssessments(assessmentRepository.count())
                .totalTriggers(triggerRepository.count())
                .totalRecoveryActions(recoveryRepository.count())
                .build();
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
