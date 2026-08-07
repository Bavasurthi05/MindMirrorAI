package com.project.mentalhealth.interfaces.api.v1.social.dto;

import com.project.mentalhealth.domain.model.SocialAccount;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SocialAccountResponse {
    private final Long id;
    private final String provider;
    private final String externalAccountId;
    private final String displayName;
    private final String profileImageUrl;
    private final String status;
    private final Instant connectedAt;
    private final Instant lastSyncedAt;

    public static SocialAccountResponse from(SocialAccount account) {
        return SocialAccountResponse.builder()
                .id(account.getId())
                .provider(account.getProvider())
                .externalAccountId(account.getExternalAccountId())
                .displayName(account.getDisplayName())
                .profileImageUrl(account.getProfileImageUrl())
                .status(account.getStatus())
                .connectedAt(account.getConnectedAt())
                .lastSyncedAt(account.getLastSyncedAt())
                .build();
    }
}
