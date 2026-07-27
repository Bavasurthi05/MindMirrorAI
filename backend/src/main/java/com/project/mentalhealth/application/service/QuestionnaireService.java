package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.QuestionnaireUseCase;
import com.project.mentalhealth.domain.model.AssessmentSubmission;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.questionnaire.dto.QuestionnaireResultResponse;
import com.project.mentalhealth.interfaces.api.v1.questionnaire.dto.QuestionnaireSubmitRequest;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionnaireService implements QuestionnaireUseCase {

    private final AssessmentSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public QuestionnaireService(AssessmentSubmissionRepository submissionRepository, UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public QuestionnaireResultResponse submit(String userEmail, QuestionnaireSubmitRequest request) {
        User user = requireUser(userEmail);

        List<Integer> answers = request.getAnswers();
        int optionsPerQuestion = request.getOptionsPerQuestion() > 1 ? request.getOptionsPerQuestion() : 4;
        int totalScore = answers.stream().mapToInt(Integer::intValue).sum();
        int maxScore = answers.size() * (optionsPerQuestion - 1);

        AssessmentSubmission submission = new AssessmentSubmission();
        submission.setUser(user);
        submission.setQuestionnaireKey(request.getQuestionnaireKey() != null ? request.getQuestionnaireKey() : "wellbeing");
        submission.setTotalScore(totalScore);
        submission.setMaxScore(maxScore);
        submission.setSeverity(classify(totalScore, maxScore));
        submission.setAnswers(answers.stream().map(String::valueOf).collect(Collectors.joining(",")));
        submission.setSubmittedAt(Instant.now());

        return QuestionnaireResultResponse.from(submissionRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionnaireResultResponse> history(String userEmail) {
        User user = requireUser(userEmail);
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream()
                .map(QuestionnaireResultResponse::from)
                .toList();
    }

    private String classify(int totalScore, int maxScore) {
        if (maxScore <= 0) {
            return "Unknown";
        }
        double ratio = (double) totalScore / maxScore;
        if (ratio >= 0.75) {
            return "Thriving";
        } else if (ratio >= 0.5) {
            return "Steady";
        } else if (ratio >= 0.25) {
            return "Strained";
        }
        return "At risk";
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
