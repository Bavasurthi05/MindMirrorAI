package com.project.mentalhealth.shared.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    public static Instant nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC).toInstant();
    }
}
