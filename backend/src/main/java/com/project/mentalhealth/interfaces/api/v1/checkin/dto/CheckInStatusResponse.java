package com.project.mentalhealth.interfaces.api.v1.checkin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CheckInStatusResponse {
    private final boolean checkedInToday;
    /** Today in the user's own timezone, not the server's. */
    private final String localDate;
    private final String timezone;
    private final Instant lastCheckInAt;
    private final Integer lastMoodScore;
}
