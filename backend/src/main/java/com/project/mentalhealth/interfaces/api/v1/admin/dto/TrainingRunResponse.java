package com.project.mentalhealth.interfaces.api.v1.admin.dto;

import com.project.mentalhealth.application.ports.out.MlTrainingPort;
import com.project.mentalhealth.domain.model.ModelTrainingRun;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/** A retraining run: what it learned from, how it scored, and whether it was deployed. */
@Getter
@Builder
public class TrainingRunResponse {

    private final Long id;
    private final String jobId;
    private final String version;
    private final String status;
    private final String triggeredBy;
    private final int corpusTotal;
    private final int corpusUserExamples;
    private final Double accuracy;
    private final Double f1Macro;
    private final boolean gatePassed;
    private final List<GateCheck> gateChecks;
    private final List<String> gateFailures;
    private final boolean promoted;
    private final Instant promotedAt;
    private final String promotedBy;
    private final boolean forced;
    private final String message;
    private final Instant createdAt;

    @Getter
    @Builder
    public static class GateCheck {
        private final String name;
        private final boolean passed;
        private final String detail;
    }

    public static TrainingRunResponse from(ModelTrainingRun run, MlTrainingPort.TrainJob job) {
        MlTrainingPort.Gate gate = job == null ? null : job.gate();
        return TrainingRunResponse.builder()
                .id(run.getId())
                .jobId(run.getJobId())
                .version(run.getVersion())
                .status(run.getStatus())
                .triggeredBy(run.getTriggeredBy())
                .corpusTotal(run.getCorpusTotal())
                .corpusUserExamples(run.getCorpusUserExamples())
                .accuracy(run.getAccuracy())
                .f1Macro(run.getF1Macro())
                .gatePassed(run.isGatePassed())
                .gateChecks(gate == null ? List.of() : gate.checks().stream()
                        .map(check -> GateCheck.builder()
                                .name(check.name())
                                .passed(check.passed())
                                .detail(check.detail())
                                .build())
                        .toList())
                .gateFailures(gate == null ? List.of() : gate.failed())
                .promoted(run.isPromoted())
                .promotedAt(run.getPromotedAt())
                .promotedBy(run.getPromotedBy())
                .forced(run.isForced())
                .message(run.getMessage())
                .createdAt(run.getCreatedAt())
                .build();
    }
}
