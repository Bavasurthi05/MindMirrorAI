package com.project.mentalhealth.domain.model;

/** Lifecycle of an uploaded export. */
public enum SocialImportStatus {
    /** Parsed and saved; posts are being analyzed in the background. */
    PROCESSING,
    /** Every post has been through analysis (some may still be retrying). */
    COMPLETED,
    /** Analysis could not run at all. */
    FAILED
}
