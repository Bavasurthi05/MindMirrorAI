package com.project.mentalhealth.interfaces.api.v1.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * The composite wellbeing score, with everything needed to explain it.
 *
 * <p>{@code score} is null when too few signals exist to compose one — the client shows an
 * empty state rather than a fabricated number.
 */
@Getter
@Builder
public class MindsetResponse {

    private final Integer score;
    private final String band;
    private final List<Component> components;

    /** 0-1: how much evidence stands behind the score, not how sure a model is. */
    private final double confidence;

    private final boolean baselineEstablished;
    private final int baselineDaysCovered;
    private final Double baselineMeanSentiment;
    private final Double baselineMeanMood;

    /** Standard deviations from this user's own normal; null without usable spread. */
    private final Double deviationSigma;

    private final String insight;

    @Getter
    @Builder
    public static class Component {
        private final String label;
        private final int value;
        /** Renormalized share of the score, so the components always sum to 1. */
        private final double weight;
        private final double contribution;
        private final String detail;
    }
}
