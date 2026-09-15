package com.project.mentalhealth.application.service;

/** Raised when an uploaded export's posts are saved and ready to analyze. */
public record SocialImportRequestedEvent(Long importId) {
}
