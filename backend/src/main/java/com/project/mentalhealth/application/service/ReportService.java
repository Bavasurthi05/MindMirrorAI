package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.ReportUseCase;
import com.project.mentalhealth.domain.model.AssessmentSubmission;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.RecoveryAction;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.report.dto.ReportSummaryResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService implements ReportUseCase {

    private final UserRepository userRepository;
    private final JournalEntryRepository journalRepository;
    private final MoodEntryRepository moodRepository;
    private final TriggerEntryRepository triggerRepository;
    private final AssessmentSubmissionRepository assessmentRepository;
    private final RecoveryActionRepository recoveryRepository;

    public ReportService(UserRepository userRepository,
                         JournalEntryRepository journalRepository,
                         MoodEntryRepository moodRepository,
                         TriggerEntryRepository triggerRepository,
                         AssessmentSubmissionRepository assessmentRepository,
                         RecoveryActionRepository recoveryRepository) {
        this.userRepository = userRepository;
        this.journalRepository = journalRepository;
        this.moodRepository = moodRepository;
        this.triggerRepository = triggerRepository;
        this.assessmentRepository = assessmentRepository;
        this.recoveryRepository = recoveryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportSummaryResponse summary(String userEmail) {
        User user = requireUser(userEmail);
        Long userId = user.getId();

        List<MoodEntry> moods = moodRepository.findByUserIdOrderByRecordedAtDesc(userId);
        double averageMood = moods.stream().mapToInt(MoodEntry::getMoodScore).average().orElse(0);

        List<AssessmentSubmission> assessments = assessmentRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        AssessmentSubmission latest = assessments.stream().findFirst().orElse(null);
        String latestSeverity = latest != null ? latest.getSeverity() : "Not assessed";

        int overallWellness;
        if (latest != null && latest.getMaxScore() > 0) {
            overallWellness = (int) Math.round(100.0 * latest.getTotalScore() / latest.getMaxScore());
        } else {
            overallWellness = (int) Math.round(averageMood);
        }

        List<TriggerEntry> triggers = triggerRepository.findByUserIdOrderByOccurredAtDesc(userId);
        double avgTriggerIntensity = triggers.stream().mapToInt(TriggerEntry::getIntensity).average().orElse(0);
        String stressTrend = avgTriggerIntensity >= 7 ? "High" : avgTriggerIntensity >= 4 ? "Medium" : "Low";

        Map<String, Double> triggerByCategory = triggers.stream()
                .collect(Collectors.groupingBy(TriggerEntry::getCategory,
                        Collectors.averagingInt(TriggerEntry::getIntensity)));
        List<ReportSummaryResponse.TriggerStrength> topTriggers = triggerByCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> ReportSummaryResponse.TriggerStrength.builder()
                        .title(e.getKey())
                        .strength(e.getValue() >= 7 ? "High" : e.getValue() >= 4 ? "Medium" : "Low")
                        .build())
                .toList();

        List<RecoveryAction> recoveryActions = recoveryRepository.findByUserIdOrderByIdAsc(userId);
        long completed = recoveryActions.stream().filter(RecoveryAction::isCompleted).count();
        int recoveryScore = recoveryActions.isEmpty() ? 0
                : (int) Math.round(100.0 * completed / recoveryActions.size());

        List<String> recommendations = recoveryActions.stream()
                .filter(action -> !action.isCompleted())
                .map(RecoveryAction::getTitle)
                .limit(3)
                .toList();

        long journalCount = journalRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1))
                .getTotalElements();

        List<String> predictionSummary = List.of(
                String.format("Mood pattern suggests an average score of %.0f/100 across recent entries.", averageMood),
                stressTrend.equals("High")
                        ? "Elevated trigger intensity detected — prioritizing recovery actions is recommended."
                        : "Trigger intensity is within a manageable range.",
                recoveryScore >= 50
                        ? "Strong engagement with your recovery plan is supporting steadier wellbeing."
                        : "Completing more recovery actions could strengthen your progress."
        );

        String summaryText = String.format(
                "Based on %d journal entr%s, %d mood check-in%s, and your latest assessment (%s), "
                        + "your overall wellbeing is estimated at %d/100 with a %s stress trend.",
                journalCount, journalCount == 1 ? "y" : "ies",
                moods.size(), moods.size() == 1 ? "" : "s",
                latestSeverity, overallWellness, stressTrend.toLowerCase());

        return ReportSummaryResponse.builder()
                .overallWellness(overallWellness)
                .stressTrend(stressTrend)
                .recoveryScore(recoveryScore)
                .journalCount(journalCount)
                .averageMood(Math.round(averageMood * 10.0) / 10.0)
                .latestSeverity(latestSeverity)
                .summaryText(summaryText)
                .topTriggers(topTriggers)
                .recommendations(recommendations)
                .predictionSummary(predictionSummary)
                .build();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
