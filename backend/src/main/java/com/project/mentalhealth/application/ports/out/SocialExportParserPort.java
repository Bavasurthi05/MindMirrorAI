package com.project.mentalhealth.application.ports.out;

import com.project.mentalhealth.domain.model.SocialProvider;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * Reads the posts a user wrote out of a platform's data export.
 *
 * <p>Implementations must only return the user's own words: never direct messages, never
 * other people's replies, never reposts of someone else's content. Those belong to people
 * who have not consented to having their mental health analyzed.
 */
public interface SocialExportParserPort {

    /**
     * @param provider which platform the export came from
     * @param filename the uploaded name, used only for error messages
     * @param content  a ZIP archive, or a single extracted posts file
     * @throws com.project.mentalhealth.shared.exception.ApiException when the file is not a
     *         recognisable export, or exceeds the safety limits
     */
    ParsedExport parse(SocialProvider provider, String filename, InputStream content);

    /**
     * @param externalId the platform's id, or a stable hash where the export has none
     * @param postedAt   null when the export carried no usable date
     */
    record ParsedPost(String externalId, String text, Instant postedAt) {}

    /**
     * @param posts          the user's own posts that carry text
     * @param skippedReposts reposts of other people's content, left out
     * @param skippedNoText  posts with no caption or text, such as photo-only posts
     */
    record ParsedExport(List<ParsedPost> posts, int skippedReposts, int skippedNoText) {}
}
