package com.project.mentalhealth.interfaces.api.v1.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private final String accessToken;
    private final String tokenType;
    private final String expiresIn;
}
