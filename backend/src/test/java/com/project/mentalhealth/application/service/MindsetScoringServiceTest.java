package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.UserBaseline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The composite score's arithmetic and its refusal to guess.
 *
 * <p>Deliberately unit-level: the weighting and null-handling rules are the part that must
 * not drift, and they are pure functions of the components.
 */
class MindsetScoringServiceTest {

    private static MindsetScoringService.Component component(String label, double weight, double value) {
        return new MindsetScoringService.Component(label, weight, value, "");
    }

    @Test
    void aSingleComponentIsNotACompositeScore() {
        assertThat(MindsetScoringService.compose(List.of(component("Mood", 0.25, 80)))).isNull();
    }

    @Test
    void noComponentsProducesNoScore() {
        assertThat(MindsetScoringService.compose(List.of())).isNull();
    }

    @Test
    void twoComponentsAreEnough() {
        Integer score = MindsetScoringService.compose(List.of(
                component("Mood", 0.25, 80),
                component("Reflection tone", 0.25, 60)));

        assertThat(score).isEqualTo(70);
    }

    @Test
    void missingComponentsRenormalizeTheRemainingWeights() {
        // Weights 0.25 and 0.20 sum to 0.45, not 1.0 — the result must still be on the
        // 0-100 scale rather than scaled down by the missing 0.55.
        Integer score = MindsetScoringService.compose(List.of(
                component("Mood", 0.25, 100),
                component("Trigger load", 0.20, 100)));

        assertThat(score).isEqualTo(100);
    }

    @Test
    void weightsActuallyShiftTheResult() {
        Integer heavyMood = MindsetScoringService.compose(List.of(
                component("Mood", 0.80, 100),
                component("Trigger load", 0.20, 0)));

        assertThat(heavyMood).isEqualTo(80);
    }

    @Test
    void allComponentsTogetherAverageAsExpected() {
        Integer score = MindsetScoringService.compose(List.of(
                component("Reflection tone", MindsetScoringService.WEIGHT_SENTIMENT, 60),
                component("Mood", MindsetScoringService.WEIGHT_MOOD, 60),
                component("Trigger load", MindsetScoringService.WEIGHT_TRIGGERS, 60),
                component("Questionnaire", MindsetScoringService.WEIGHT_ASSESSMENT, 60),
                component("Recovery engagement", MindsetScoringService.WEIGHT_RECOVERY, 60)));

        assertThat(score).isEqualTo(60);
    }

    @Test
    void zeroTotalWeightDoesNotDivideByZero() {
        assertThat(MindsetScoringService.compose(List.of(
                component("A", 0.0, 50),
                component("B", 0.0, 50)))).isNull();
    }

    @Test
    void bandsCoverTheWholeRange() {
        assertThat(MindsetScoringService.band(90)).isEqualTo("Thriving");
        assertThat(MindsetScoringService.band(65)).isEqualTo("Steady");
        assertThat(MindsetScoringService.band(45)).isEqualTo("Strained");
        assertThat(MindsetScoringService.band(20)).isEqualTo("Struggling");
    }

    @Test
    void thereIsNoBandWithoutAScore() {
        assertThat(MindsetScoringService.band(null)).isNull();
    }

    @Test
    void confidenceIsZeroWithoutEnoughComponents() {
        assertThat(MindsetScoringService.confidence(List.of(component("Mood", 0.25, 80)), null))
                .isZero();
    }

    @Test
    void confidenceRisesWithCoverageAndHistory() {
        UserBaseline young = new UserBaseline();
        young.setDaysCovered(2);
        young.setEstablished(false);

        UserBaseline mature = new UserBaseline();
        mature.setDaysCovered(60);
        mature.setEstablished(true);

        List<MindsetScoringService.Component> two = List.of(
                component("Mood", 0.25, 80), component("Trigger load", 0.20, 70));
        List<MindsetScoringService.Component> five = List.of(
                component("A", 0.25, 80), component("B", 0.25, 80), component("C", 0.20, 80),
                component("D", 0.20, 80), component("E", 0.10, 80));

        assertThat(MindsetScoringService.confidence(five, mature))
                .isGreaterThan(MindsetScoringService.confidence(two, young));
        assertThat(MindsetScoringService.confidence(five, mature)).isEqualTo(1.0);
    }

    @Test
    void insightAsksForMoreDataWhenThereIsNoScore() {
        assertThat(MindsetScoringService.insight(null, null, null, 0))
                .contains("Add a couple more entries");
    }

    @Test
    void insightSaysItIsStillLearningBeforeABaselineExists() {
        UserBaseline baseline = new UserBaseline();
        baseline.setEstablished(false);

        assertThat(MindsetScoringService.insight(70, 1.5, baseline, 3))
                .contains("Still learning what's normal for you");
    }

    @Test
    void insightIsFramedAsDeviationFromTheUsersOwnRange() {
        UserBaseline baseline = new UserBaseline();
        baseline.setEstablished(true);

        String insight = MindsetScoringService.insight(40, -1.4, baseline, 4);

        assertThat(insight).contains("1.4 standard deviations below your usual range");
    }

    @Test
    void smallDeviationsReadAsNormalRatherThanAlarming() {
        UserBaseline baseline = new UserBaseline();
        baseline.setEstablished(true);

        assertThat(MindsetScoringService.insight(60, 0.4, baseline, 4))
                .isEqualTo("This is within your usual range.");
    }
}
