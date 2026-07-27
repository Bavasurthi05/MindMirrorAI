package com.project.mentalhealth.interfaces.api.v1.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;
    private final Long userId;
    private final String email;
    private final String fullName;
    private final String role;
    private final boolean emailVerified;
}
