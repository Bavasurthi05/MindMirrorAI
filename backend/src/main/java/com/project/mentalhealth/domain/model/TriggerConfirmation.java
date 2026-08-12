package com.project.mentalhealth.domain.model;

/** Review state of a trigger the ML service detected in the user's writing. */
public enum TriggerConfirmation {
    /** User-logged triggers need no review. */
    NOT_REQUIRED,
    /** Detected automatically, awaiting the user's verdict. */
    PENDING,
    /** User agreed this was a real trigger. */
    CONFIRMED,
    /** User rejected it — kept out of analytics, retained as negative signal. */
    DISMISSED
}
