package com.project.mentalhealth.domain.model;

/**
 * Provenance of a mood or trigger entry.
 *
 * <p>Stored as a plain string rather than an enum so existing rows (defaulted by migration V9)
 * stay valid and future sources can be added without a schema change.
 */
public final class EntrySource {

    /** The user logged it directly. */
    public static final String SELF_REPORTED = "SELF_REPORTED";

    /** Inferred by the ML service from the user's text. */
    public static final String DERIVED = "DERIVED";

    /** Trigger the user logged by hand. */
    public static final String USER_LOGGED = "USER_LOGGED";

    private EntrySource() {
    }
}
