package com.project.mentalhealth.interfaces.api.v1.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** Volume and class balance of prediction feedback — the readiness signal for retraining. */
@Getter
@Builder
public class FeedbackStatsResponse {

    private final long total;
    private final long agree;
    private final long disagree;
    private final long partial;

    /** Null when no feedback exists yet, rather than a misleading 0%. */
    private final Double agreementRatePercent;

    private final Map<String, Long> correctedLabelCounts;
}
