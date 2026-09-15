package com.project.mentalhealth.domain.model;

import java.util.Locale;
import java.util.Optional;

/** Platforms whose data export can be uploaded. */
public enum SocialProvider {
    X,
    INSTAGRAM,
    FACEBOOK;

    /** Accepts the names people actually type, including the platform's old name. */
    public static Optional<SocialProvider> fromParam(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "x", "twitter" -> Optional.of(X);
            case "instagram" -> Optional.of(INSTAGRAM);
            case "facebook" -> Optional.of(FACEBOOK);
            default -> Optional.empty();
        };
    }
}
