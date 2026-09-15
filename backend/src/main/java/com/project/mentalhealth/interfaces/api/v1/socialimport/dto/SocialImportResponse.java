package com.project.mentalhealth.interfaces.api.v1.socialimport.dto;

import com.project.mentalhealth.domain.model.SocialImport;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/** An upload and how far its analysis has got. */
@Getter
@Builder
public class SocialImportResponse {

    private final Long id;
    private final String provider;
    private final String originalFilename;
    private final String status;

    /** Posts found in the file before any filtering. */
    private final int postsFound;
    private final int postsImported;
    private final int skippedDuplicates;
    private final int skippedReposts;
    private final int skippedShort;
    private final int skippedOverLimit;

    private final long postsAnalyzed;
    /** Analyses that failed; the retry job tries these again. */
    private final long postsFailed;
    private final long postsPending;

    private final Instant earliestPostAt;
    private final Instant latestPostAt;
    private final Instant createdAt;
    private final Instant completedAt;
    private final String errorMessage;

    public static SocialImportResponse from(SocialImport socialImport, long analyzed, long failed, long pending) {
        return SocialImportResponse.builder()
                .id(socialImport.getId())
                .provider(socialImport.getProvider().name())
                .originalFilename(socialImport.getOriginalFilename())
                .status(socialImport.getStatus().name())
                .postsFound(socialImport.getPostsFound())
                .postsImported(socialImport.getPostsImported())
                .skippedDuplicates(socialImport.getSkippedDuplicates())
                .skippedReposts(socialImport.getSkippedReposts())
                .skippedShort(socialImport.getSkippedShort())
                .skippedOverLimit(socialImport.getSkippedOverLimit())
                .postsAnalyzed(analyzed)
                .postsFailed(failed)
                .postsPending(pending)
                .earliestPostAt(socialImport.getEarliestPostAt())
                .latestPostAt(socialImport.getLatestPostAt())
                .createdAt(socialImport.getCreatedAt())
                .completedAt(socialImport.getCompletedAt())
                .errorMessage(socialImport.getErrorMessage())
                .build();
    }
}
