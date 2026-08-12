package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.FeedbackAgreement;
import com.project.mentalhealth.domain.model.PredictionFeedback;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.PredictionFeedbackRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.FeedbackStatsResponse;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.PredictionFeedbackRequest;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.PredictionFeedbackResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PredictionFeedbackServiceTest {

    @Mock private PredictionFeedbackRepository feedbackRepository;
    @Mock private AnalysisResultRepository analysisRepository;
    @Mock private UserRepository userRepository;

    private PredictionFeedbackService service;
    private AnalysisResult analysis;

    @BeforeEach
    void setUp() {
        service = new PredictionFeedbackService(feedbackRepository, analysisRepository, userRepository);

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

        analysis = new AnalysisResult();
        analysis.setId(5L);
        analysis.setUser(user);
        analysis.setPrediction("stress");
        given(analysisRepository.findByIdAndUserId(5L, 1L)).willReturn(Optional.of(analysis));
        given(feedbackRepository.findByAnalysisResultId(anyLong())).willReturn(Optional.empty());
        given(feedbackRepository.save(any(PredictionFeedback.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private PredictionFeedbackRequest request(String agreement, String corrected) {
        PredictionFeedbackRequest request = new PredictionFeedbackRequest();
        request.setAgreement(agreement);
        request.setCorrectedLabel(corrected);
        return request;
    }

    @Test
    void agreeingSnapshotsThePredictedLabel() {
        PredictionFeedbackResponse response = service.submit("user@example.com", 5L, request("AGREE", null));

        assertThat(response.getAgreement()).isEqualTo("AGREE");
        assertThat(response.getPredictedLabel()).isEqualTo("stress");
        assertThat(response.getCorrectedLabel()).isNull();
    }

    @Test
    void agreeingIgnoresAStrayCorrection() {
        PredictionFeedbackResponse response = service.submit("user@example.com", 5L, request("AGREE", "anxiety"));

        assertThat(response.getCorrectedLabel()).isNull();
    }

    @Test
    void disagreeingRecordsTheCorrectedLabel() {
        PredictionFeedbackResponse response = service.submit("user@example.com", 5L, request("disagree", "Anxiety"));

        assertThat(response.getAgreement()).isEqualTo("DISAGREE");
        assertThat(response.getCorrectedLabel()).isEqualTo("anxiety");
    }

    @Test
    void disagreeingWithoutACorrectionIsRejected() {
        assertThatThrownBy(() -> service.submit("user@example.com", 5L, request("DISAGREE", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("which state");
    }

    @Test
    void aLabelOutsideTheModelsSpaceIsRejected() {
        assertThatThrownBy(() -> service.submit("user@example.com", 5L, request("DISAGREE", "burnout")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown state");
    }

    @Test
    void anUnknownAgreementValueIsRejected() {
        assertThatThrownBy(() -> service.submit("user@example.com", 5L, request("MAYBE", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("AGREE");
    }

    @Test
    void reRatingReplacesTheEarlierVerdict() {
        PredictionFeedback existing = new PredictionFeedback();
        existing.setId(77L);
        existing.setAnalysisResult(analysis);
        existing.setAgreement(FeedbackAgreement.AGREE);
        given(feedbackRepository.findByAnalysisResultId(5L)).willReturn(Optional.of(existing));

        PredictionFeedbackResponse response = service.submit("user@example.com", 5L, request("DISAGREE", "normal"));

        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getAgreement()).isEqualTo("DISAGREE");
    }

    @Test
    void feedbackOnSomeoneElsesAnalysisIsNotFound() {
        given(analysisRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit("user@example.com", 99L, request("AGREE", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void statsReportNullAgreementRateBeforeAnyFeedback() {
        given(feedbackRepository.countByAgreement(any())).willReturn(0L);
        given(feedbackRepository.countByCorrectedLabel()).willReturn(List.of());

        FeedbackStatsResponse stats = service.stats();

        assertThat(stats.getTotal()).isZero();
        assertThat(stats.getAgreementRatePercent()).isNull();
    }

    @Test
    void statsComputeAgreementRateAndClassBalance() {
        given(feedbackRepository.countByAgreement(FeedbackAgreement.AGREE)).willReturn(7L);
        given(feedbackRepository.countByAgreement(FeedbackAgreement.DISAGREE)).willReturn(3L);
        given(feedbackRepository.countByAgreement(FeedbackAgreement.PARTIAL)).willReturn(0L);
        given(feedbackRepository.countByCorrectedLabel())
                .willReturn(List.of(new Object[]{"anxiety", 2L}, new Object[]{"normal", 1L}));

        FeedbackStatsResponse stats = service.stats();

        assertThat(stats.getTotal()).isEqualTo(10);
        assertThat(stats.getAgreementRatePercent()).isEqualTo(70.0);
        assertThat(stats.getCorrectedLabelCounts()).containsEntry("anxiety", 2L);
    }
}
