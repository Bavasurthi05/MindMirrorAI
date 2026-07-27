package com.project.mentalhealth.interfaces.api.v1.profile.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ProfileResponse {
    private final String fullName;
    private final String email;
    private final String role;
    private final boolean emailVerified;
    private final Instant memberSince;
    private final int journalStreak;
    private final long journalCount;
    private final long moodCount;
    private final long goalsCompleted;
    private final long goalsTotal;
}
