package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.RecoveryUseCase;
import com.project.mentalhealth.domain.model.AssessmentSubmission;
import com.project.mentalhealth.domain.model.RecoveryAction;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.recovery.dto.RecoveryActionResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecoveryService implements RecoveryUseCase {

    private final RecoveryActionRepository recoveryRepository;
    private final AssessmentSubmissionRepository assessmentRepository;
    private final UserRepository userRepository;

    public RecoveryService(RecoveryActionRepository recoveryRepository,
                           AssessmentSubmissionRepository assessmentRepository,
                           UserRepository userRepository) {
        this.recoveryRepository = recoveryRepository;
        this.assessmentRepository = assessmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public List<RecoveryActionResponse> getPlan(String userEmail) {
        User user = requireUser(userEmail);
        if (recoveryRepository.countByUserId(user.getId()) == 0) {
            seedPlan(user);
        }
        return recoveryRepository.findByUserIdOrderByIdAsc(user.getId())
                .stream()
                .map(RecoveryActionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public RecoveryActionResponse toggle(String userEmail, Long actionId) {
        User user = requireUser(userEmail);
        RecoveryAction action = recoveryRepository.findByIdAndUserId(actionId, user.getId())
                .orElseThrow(() -> new ApiException("Recovery action not found", HttpStatus.NOT_FOUND));
        action.setCompleted(!action.isCompleted());
        return RecoveryActionResponse.from(recoveryRepository.save(action));
    }

    private void seedPlan(User user) {
        String severity = assessmentRepository.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream()
                .findFirst()
                .map(AssessmentSubmission::getSeverity)
                .orElse("Steady");

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(action(user, "Meditation", "Reduce stress and improve presence", "10 min",
                "A short guided meditation can help settle the mind after a demanding day."));
        actions.add(action(user, "Breathing Exercises", "Calm the nervous system", "5 min",
                "Try slow inhale-exhale cycles to regain steadiness and ease tension."));
        actions.add(action(user, "Journaling", "Reflect on emotions and patterns", "8 min",
                "Capture thoughts in a simple entry to make feelings easier to understand."));
        actions.add(action(user, "Walking", "Support energy and mood", "20 min",
                "A gentle walk can bring clarity, movement, and fresh perspective."));
        actions.add(action(user, "Sleep Improvement", "Create a calmer wind-down", "30 min before bed",
                "Reduce screen time and keep the environment dim to support rest."));

        if ("Strained".equalsIgnoreCase(severity) || "At risk".equalsIgnoreCase(severity)) {
            actions.add(action(user, "Professional Consultation", "Add expert support when useful", "As needed",
                    "A professional conversation can be a strong next step if emotions feel overwhelming."));
            actions.add(action(user, "Daily Check-in", "Build a supportive routine", "3 min",
                    "Log your mood each day to notice early signs and stay connected to your progress."));
        }

        recoveryRepository.saveAll(actions);
    }

    private RecoveryAction action(User user, String title, String focus, String duration, String description) {
        RecoveryAction action = new RecoveryAction();
        action.setUser(user);
        action.setTitle(title);
        action.setFocus(focus);
        action.setDuration(duration);
        action.setDescription(description);
        action.setCompleted(false);
        return action;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
