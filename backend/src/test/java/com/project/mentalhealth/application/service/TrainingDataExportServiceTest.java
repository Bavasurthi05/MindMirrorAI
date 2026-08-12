package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.out.MlTrainingPort;
import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.FeedbackAgreement;
import com.project.mentalhealth.domain.model.PredictionFeedback;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.model.UserPreferences;
import com.project.mentalhealth.domain.repository.PredictionFeedbackRepository;
import com.project.mentalhealth.domain.repository.UserPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainingDataExportServiceTest {

    @Mock private PredictionFeedbackRepository feedbackRepository;
    @Mock private UserPreferencesRepository preferencesRepository;

    private TrainingDataExportService service;

    @BeforeEach
    void setUp() {
        service = new TrainingDataExportService(feedbackRepository, preferencesRepository, "test-salt", 200);
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }

    private void consent(long userId, boolean granted) {
        UserPreferences preferences = new UserPreferences();
        preferences.setTrainingConsent(granted);
        given(preferencesRepository.findByUserId(userId)).willReturn(Optional.of(preferences));
    }

    private PredictionFeedback feedback(User owner, String text, FeedbackAgreement agreement,
                                        String predicted, String corrected) {
        AnalysisResult analysis = new AnalysisResult();
        analysis.setId(1L);
        analysis.setUser(owner);
        analysis.setSourceText(text);

        PredictionFeedback item = new PredictionFeedback();
        item.setUser(owner);
        item.setAnalysisResult(analysis);
        item.setAgreement(agreement);
        item.setPredictedLabel(predicted);
        item.setCorrectedLabel(corrected);
        return item;
    }

    @Test
    void withoutConsentNothingIsExported() {
        User owner = user(1);
        consent(1, false);
        given(feedbackRepository.findAll()).willReturn(List.of(
                feedback(owner, "a long enough reflection about my week", FeedbackAgreement.AGREE, "stress", null)));

        assertThat(service.export()).isEmpty();
    }

    @Test
    void aMissingPreferencesRowCountsAsNoConsent() {
        User owner = user(1);
        given(preferencesRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(feedbackRepository.findAll()).willReturn(List.of(
                feedback(owner, "a long enough reflection about my week", FeedbackAgreement.AGREE, "stress", null)));

        assertThat(service.export()).isEmpty();
    }

    @Test
    void consentedFeedbackIsExportedWithTheTaughtLabel() {
        User owner = user(1);
        consent(1, true);
        given(feedbackRepository.findAll()).willReturn(List.of(
                feedback(owner, "a long enough reflection about my week", FeedbackAgreement.DISAGREE, "stress", "anxiety")));

        List<MlTrainingPort.TrainingExample> exported = service.export();

        assertThat(exported).hasSize(1);
        assertThat(exported.get(0).label()).isEqualTo("anxiety");
        assertThat(exported.get(0).source()).isEqualTo("user_correction");
    }

    @Test
    void correctionsOutweighAgreements() {
        User owner = user(1);
        consent(1, true);
        given(feedbackRepository.findAll()).willReturn(List.of(
                feedback(owner, "the first sufficiently long reflection here", FeedbackAgreement.DISAGREE, "stress", "anxiety"),
                feedback(owner, "a second sufficiently long reflection here", FeedbackAgreement.AGREE, "normal", null)));

        List<MlTrainingPort.TrainingExample> exported = service.export();

        double correctionWeight = exported.stream()
                .filter(e -> e.source().equals("user_correction")).findFirst().orElseThrow().weight();
        double agreementWeight = exported.stream()
                .filter(e -> e.source().equals("user_agreement")).findFirst().orElseThrow().weight();
        assertThat(correctionWeight).isGreaterThan(agreementWeight);
    }

    @Test
    void ambiguousPartialAnswersAreNotTrainedOn() {
        User owner = user(1);
        consent(1, true);
        given(feedbackRepository.findAll()).willReturn(List.of(
                feedback(owner, "a long enough reflection about my week", FeedbackAgreement.PARTIAL, "stress", null)));

        assertThat(service.export()).isEmpty();
    }

    @Test
    void identifiersNeverLeaveTheBackend() {
        User owner = user(1);
        consent(1, true);
        given(feedbackRepository.findAll()).willReturn(List.of(
                feedback(owner, "a long enough reflection about my week", FeedbackAgreement.AGREE, "stress", null)));

        MlTrainingPort.TrainingExample example = service.export().get(0);

        assertThat(example.userHash()).isNotEmpty();
        assertThat(example.userHash()).doesNotContain("user1@example.com");
        assertThat(example.userHash()).doesNotContain("1");
    }

    @Test
    void tooShortTextIsDropped() {
        User owner = user(1);
        consent(1, true);
        given(feedbackRepository.findAll()).willReturn(List.of(
                feedback(owner, "too short", FeedbackAgreement.AGREE, "stress", null)));

        assertThat(service.export()).isEmpty();
    }

    @Test
    void duplicateTextIsExportedOnce() {
        User owner = user(1);
        consent(1, true);
        String text = "the very same reflection text repeated";
        given(feedbackRepository.findAll()).willReturn(List.of(
                feedback(owner, text, FeedbackAgreement.AGREE, "stress", null),
                feedback(owner, text, FeedbackAgreement.AGREE, "stress", null)));

        assertThat(service.export()).hasSize(1);
    }

    @Test
    void oneUserCannotDominateTheCorpus() {
        service = new TrainingDataExportService(feedbackRepository, preferencesRepository, "test-salt", 3);
        User owner = user(1);
        consent(1, true);

        List<PredictionFeedback> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            rows.add(feedback(owner, "a distinct long reflection number " + i, FeedbackAgreement.AGREE, "stress", null));
        }
        given(feedbackRepository.findAll()).willReturn(rows);

        assertThat(service.export()).hasSize(3);
    }

    // --- PII scrubbing ------------------------------------------------------------------

    @Test
    void scrubRemovesEmailsAndUrls() {
        String scrubbed = TrainingDataExportService.scrub(
                "email me at ada@example.com or see https://example.com/notes for details");

        assertThat(scrubbed).doesNotContain("ada@example.com");
        assertThat(scrubbed).doesNotContain("https://example.com");
        assertThat(scrubbed).contains("email me at");
    }

    @Test
    void scrubRemovesHandlesAndPhoneNumbers() {
        String scrubbed = TrainingDataExportService.scrub(
                "spoke to @charles about #work, call me on +61 400 123 456");

        assertThat(scrubbed).doesNotContain("@charles");
        assertThat(scrubbed).doesNotContain("#work");
        assertThat(scrubbed).doesNotContain("400");
        assertThat(scrubbed).contains("spoke to");
    }

    @Test
    void scrubKeepsTheActualContent() {
        String scrubbed = TrainingDataExportService.scrub("I felt anxious and overwhelmed all week");

        assertThat(scrubbed).isEqualTo("I felt anxious and overwhelmed all week");
    }

    @Test
    void scrubHandlesEmptyInput() {
        assertThat(TrainingDataExportService.scrub(null)).isNull();
        assertThat(TrainingDataExportService.scrub("   ")).isNull();
    }
}
