package com.project.mentalhealth.interfaces.api.v1.admin.dto;

import com.project.mentalhealth.domain.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AdminUserResponse {
    private final Long id;
    private final String fullName;
    private final String email;
    private final String role;
    private final boolean enabled;
    private final boolean emailVerified;
    private final Instant createdAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() == null ? "ROLE_USER" : user.getRole().getName())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
