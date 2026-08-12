package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.FeedbackAgreement;
import com.project.mentalhealth.domain.model.PredictionFeedback;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.PredictionFeedbackRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.PredictionFeedbackRequest;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.PredictionFeedbackResponse;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.FeedbackStatsResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures whether users agreed with a prediction, and what the right answer was.
 *
 * <p>These are the labels a retraining pipeline will learn from, so quality matters more than
 * volume: a correction is only accepted for a label the model can actually predict, and one
 * verdict is kept per analysis.
 */
@Service
public class PredictionFeedbackService {

    /** The model's label space. A correction outside it would be untrainable noise. */
    private static final Set<String> VALID_LABELS = Set.of("normal", "stress", "anxiety", "depression");

    private final PredictionFeedbackRepository feedbackRepository;
    private final AnalysisResultRepository analysisRepository;
    private final UserRepository userRepository;

    public PredictionFeedbackService(PredictionFeedbackRepository feedbackRepository,
                                     AnalysisResultRepository analysisRepository,
                                     UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.analysisRepository = analysisRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PredictionFeedbackResponse submit(String userEmail, Long analysisId, PredictionFeedbackRequest request) {
        User user = requireUser(userEmail);
        AnalysisResult analysis = analysisRepository.findByIdAndUserId(analysisId, user.getId())
                .orElseThrow(() -> new ApiException("Analysis not found", HttpStatus.NOT_FOUND));

        FeedbackAgreement agreement = parseAgreement(request.getAgreement());
        String corrected = normalizeLabel(request.getCorrectedLabel());

        if (agreement == FeedbackAgreement.DISAGREE && corrected == null) {
            throw new ApiException("Tell us which state fits better", HttpStatus.BAD_REQUEST);
        }
        if (agreement == FeedbackAgreement.AGREE) {
            corrected = null; // agreeing means the predicted label stands
        }

        // Re-rating replaces the earlier verdict instead of stacking a second one.
        PredictionFeedback feedback = feedbackRepository.findByAnalysisResultId(analysisId)
                .orElseGet(PredictionFeedback::new);
        feedback.setUser(user);
        feedback.setAnalysisResult(analysis);
        feedback.setPredictedLabel(analysis.getPrediction());
        feedback.setCorrectedLabel(corrected);
        feedback.setAgreement(agreement);
        feedback.setComment(request.getComment());

        return PredictionFeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public PredictionFeedbackResponse forAnalysis(String userEmail, Long analysisId) {
        User user = requireUser(userEmail);
        analysisRepository.findByIdAndUserId(analysisId, user.getId())
                .orElseThrow(() -> new ApiException("Analysis not found", HttpStatus.NOT_FOUND));
        return feedbackRepository.findByAnalysisResultId(analysisId)
                .map(PredictionFeedbackResponse::from)
                .orElse(null);
    }

    /**
     * Volume and class balance across all users.
     *
     * <p>Surfaced in the admin panel because retraining is only worth attempting once there are
     * enough corrections, spread across enough classes, to be meaningful.
     */
    @Transactional(readOnly = true)
    public FeedbackStatsResponse stats() {
        long agree = feedbackRepository.countByAgreement(FeedbackAgreement.AGREE);
        long disagree = feedbackRepository.countByAgreement(FeedbackAgreement.DISAGREE);
        long partial = feedbackRepository.countByAgreement(FeedbackAgreement.PARTIAL);

        Map<String, Long> byLabel = new LinkedHashMap<>();
        for (Object[] row : feedbackRepository.countByCorrectedLabel()) {
            byLabel.put((String) row[0], (Long) row[1]);
        }

        long total = agree + disagree + partial;
        Double agreementRate = total == 0 ? null : Math.round(1000.0 * agree / total) / 10.0;

        return FeedbackStatsResponse.builder()
                .total(total)
                .agree(agree)
                .disagree(disagree)
                .partial(partial)
                .agreementRatePercent(agreementRate)
                .correctedLabelCounts(byLabel)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PredictionFeedbackResponse> recent(int limit) {
        return feedbackRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(PredictionFeedbackResponse::from)
                .toList();
    }

    private FeedbackAgreement parseAgreement(String value) {
        try {
            return FeedbackAgreement.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ApiException("Agreement must be AGREE, DISAGREE or PARTIAL", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String normalized = label.trim().toLowerCase();
        if (!VALID_LABELS.contains(normalized)) {
            throw new ApiException("Unknown state: " + label, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
