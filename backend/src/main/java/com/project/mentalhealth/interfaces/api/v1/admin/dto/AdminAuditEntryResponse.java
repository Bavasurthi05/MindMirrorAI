package com.project.mentalhealth.interfaces.api.v1.admin.dto;

import com.project.mentalhealth.domain.model.AdminAuditLog;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/** One privileged account change: who did what, to whom, and when. */
@Getter
@Builder
public class AdminAuditEntryResponse {

    private final Long id;
    private final String action;
    private final String actorEmail;
    private final Long targetUserId;
    private final String targetEmail;
    private final String previousValue;
    private final String newValue;
    private final String detail;
    private final Instant createdAt;

    public static AdminAuditEntryResponse from(AdminAuditLog entry) {
        return AdminAuditEntryResponse.builder()
                .id(entry.getId())
                .action(entry.getAction())
                .actorEmail(entry.getActorEmail())
                .targetUserId(entry.getTargetUserId())
                .targetEmail(entry.getTargetEmail())
                .previousValue(entry.getPreviousValue())
                .newValue(entry.getNewValue())
                .detail(entry.getDetail())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
