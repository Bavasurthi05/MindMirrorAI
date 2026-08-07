package com.project.mentalhealth.interfaces.api.v1.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectSocialAccountRequest {
    @NotBlank
    private String provider;

    @NotBlank
    private String externalAccountId;

    private String displayName;

    private String profileImageUrl;

    private String scope;
}
