package com.project.mentalhealth.infrastructure.social;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mentalhealth.application.ports.out.SocialExportParserPort;
import com.project.mentalhealth.domain.model.SocialProvider;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Parses X, Instagram and Facebook data exports.
 *
 * <p>Accepts either the ZIP the platform provides or the extracted posts file on its own. Only
 * the user's posts files are ever opened; message files are refused by path, and reposts of
 * other people's content are dropped, because neither is the user's own writing.
 *
 * <p>Export layouts change without notice, so files are found by name wherever they sit in the
 * archive rather than at one fixed path, and each record tolerates missing fields.
 *
 * <p>Nothing is written to disk. Archives are read as a stream with a cap on the total bytes
 * inflated, which is what stops a small "zip bomb" upload from exhausting the server.
 */
@Component
public class SocialExportParser implements SocialExportParserPort {

    /** X's archive date format, e.g. "Wed Oct 10 20:19:24 +0000 2018". */
    static final DateTimeFormatter X_DATE = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

    // tweets.js, tweet.js (older archives), and tweets-part1.js etc. for large accounts.
    private static final Pattern X_POSTS_FILE = Pattern.compile("^tweets?(-part\\d+)?\\.js$");
    private static final Pattern INSTAGRAM_POSTS_FILE = Pattern.compile("^posts_\\d+\\.json$");
    private static final Pattern FACEBOOK_POSTS_FILE =
            Pattern.compile("^your_posts(__check_ins__photos_and_videos)?(_\\d+)?\\.json$");

    private static final int MAX_ZIP_ENTRIES = 50_000;
    private static final int COPY_BUFFER_BYTES = 64 * 1024;

    private final ObjectMapper objectMapper;
    private final long maxInflatedBytes;
    private final long maxFileBytes;

    @Autowired
    public SocialExportParser(ObjectMapper objectMapper,
                              @Value("${app.social-import.max-inflated-bytes:1073741824}") long maxInflatedBytes,
                              @Value("${app.social-import.max-file-bytes:52428800}") long maxFileBytes) {
        this.objectMapper = objectMapper;
        this.maxInflatedBytes = maxInflatedBytes;
        this.maxFileBytes = maxFileBytes;
    }

    @Override
    public ParsedExport parse(SocialProvider provider, String filename, InputStream content) {
        try {
            BufferedInputStream in = new BufferedInputStream(content);
            if (isZip(in)) {
                return parseArchive(provider, in);
            }
            return parseDocument(provider, readUpTo(in, maxFileBytes), displayName(filename));
        } catch (IOException ex) {
            throw new ApiException("We couldn't read that file. Try downloading your export again.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    // --- Archives ------------------------------------------------------------------------

    private ParsedExport parseArchive(SocialProvider provider, InputStream in) throws IOException {
        Accumulator accumulator = new Accumulator();
        long inflated = 0;
        int entries = 0;
        int postsFiles = 0;

        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES) {
                    throw tooLarge();
                }
                if (entry.isDirectory()) {
                    continue;
                }
                if (isPostsFile(provider, entry.getName())) {
                    byte[] bytes = readUpTo(zip, Math.min(maxFileBytes, maxInflatedBytes - inflated));
                    inflated += bytes.length;
                    accumulator.add(parseDocument(provider, bytes, displayName(entry.getName())));
                    postsFiles++;
                } else {
                    // Read through rather than closeEntry(): counting inflated bytes is the guard.
                    inflated += discard(zip, maxInflatedBytes - inflated);
                }
            }
        } catch (ZipException ex) {
            throw new ApiException("That archive is damaged or isn't a ZIP file.", HttpStatus.BAD_REQUEST);
        }

        if (postsFiles == 0) {
            throw new ApiException(missingPostsMessage(provider), HttpStatus.BAD_REQUEST);
        }
        return accumulator.result();
    }

    /** Whether an archive entry is the user's own posts for this provider. */
    static boolean isPostsFile(SocialProvider provider, String path) {
        String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        // Messages involve other people who have not consented; deleted content the user removed.
        if (normalized.contains("message") || normalized.contains("deleted")) {
            return false;
        }
        String base = normalized.substring(normalized.lastIndexOf('/') + 1);
        return switch (provider) {
            case X -> X_POSTS_FILE.matcher(base).matches();
            case INSTAGRAM -> INSTAGRAM_POSTS_FILE.matcher(base).matches();
            case FACEBOOK -> FACEBOOK_POSTS_FILE.matcher(base).matches();
        };
    }

    // --- Documents -----------------------------------------------------------------------

    private ParsedExport parseDocument(SocialProvider provider, byte[] bytes, String source) {
        JsonNode root;
        try {
            String text = new String(bytes, StandardCharsets.UTF_8);
            root = objectMapper.readTree(provider == SocialProvider.X ? stripJavascriptAssignment(text) : text);
        } catch (JsonProcessingException ex) {
            throw unreadable(provider, source);
        }
        if (root == null || !(root.isArray() || root.isObject())) {
            throw unreadable(provider, source);
        }
        return switch (provider) {
            case X -> parseX(root);
            case INSTAGRAM -> parseInstagram(root);
            case FACEBOOK -> parseFacebook(root);
        };
    }

    private ParsedExport parseX(JsonNode root) {
        List<ParsedPost> posts = new ArrayList<>();
        int reposts = 0;
        int noText = 0;
        for (JsonNode element : records(root)) {
            JsonNode tweet = element.has("tweet") ? element.get("tweet") : element;
            String text = unescapeHtml(firstText(tweet, "full_text", "text"));
            if (isBlank(text)) {
                noText++;
                continue;
            }
            // A retweet is someone else's words, not the user's.
            if (text.startsWith("RT @")) {
                reposts++;
                continue;
            }
            Instant postedAt = parseXDate(firstText(tweet, "created_at"));
            String id = firstText(tweet, "id_str", "id");
            posts.add(new ParsedPost(
                    id != null ? id : fingerprint(SocialProvider.X, postedAt, text),
                    text.trim(),
                    postedAt));
        }
        return new ParsedExport(posts, reposts, noText);
    }

    /**
     * Instagram puts the caption and date on the post for carousels, but only inside
     * {@code media[0]} for single-image posts, so both places are checked.
     */
    private ParsedExport parseInstagram(JsonNode root) {
        List<ParsedPost> posts = new ArrayList<>();
        int noText = 0;
        for (JsonNode post : records(root)) {
            JsonNode media = post.path("media");
            String caption = firstText(post, "title");
            if (isBlank(caption) && media.isArray()) {
                for (JsonNode item : media) {
                    String title = firstText(item, "title");
                    if (!isBlank(title)) {
                        caption = title;
                        break;
                    }
                }
            }
            caption = fixMetaEncoding(caption);
            if (isBlank(caption)) {
                noText++;
                continue;
            }

            Long timestamp = firstLong(post, "creation_timestamp");
            if (timestamp == null && media.isArray() && !media.isEmpty()) {
                timestamp = firstLong(media.get(0), "creation_timestamp");
            }
            Instant postedAt = toInstant(timestamp);
            posts.add(new ParsedPost(fingerprint(SocialProvider.INSTAGRAM, postedAt, caption), caption.trim(), postedAt));
        }
        return new ParsedExport(posts, 0, noText);
    }

    private ParsedExport parseFacebook(JsonNode root) {
        List<ParsedPost> posts = new ArrayList<>();
        int noText = 0;
        for (JsonNode post : records(root)) {
            StringBuilder text = new StringBuilder();
            for (JsonNode item : post.path("data")) {
                String part = fixMetaEncoding(firstText(item, "post"));
                if (!isBlank(part)) {
                    if (!text.isEmpty()) {
                        text.append('\n');
                    }
                    text.append(part.trim());
                }
            }
            if (text.isEmpty()) {
                noText++;
                continue;
            }
            Instant postedAt = toInstant(firstLong(post, "timestamp"));
            String content = text.toString();
            posts.add(new ParsedPost(fingerprint(SocialProvider.FACEBOOK, postedAt, content), content, postedAt));
        }
        return new ParsedExport(posts, 0, noText);
    }

    // --- Helpers -------------------------------------------------------------------------

    /**
     * The records array of a document: the root itself, or the first array inside an object
     * wrapper (older Facebook exports use {@code {"status_updates": [...]}}).
     */
    private static List<JsonNode> records(JsonNode root) {
        JsonNode array = root;
        if (root.isObject()) {
            array = null;
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                JsonNode value = fields.next().getValue();
                if (value.isArray()) {
                    array = value;
                    break;
                }
            }
        }
        List<JsonNode> records = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(node -> {
                if (node.isObject()) {
                    records.add(node);
                }
            });
        }
        return records;
    }

    /** X ships its data as JavaScript: {@code window.YTD.tweets.part0 = [ ... ]}. */
    static String stripJavascriptAssignment(String text) {
        String trimmed = text.stripLeading();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return trimmed;
        }
        int equals = trimmed.indexOf('=');
        return equals < 0 ? trimmed : trimmed.substring(equals + 1);
    }

    /**
     * Meta exports write each UTF-8 byte as its own Latin-1 character, so "don't" arrives as
     * "donât". Re-decode only when the result is valid UTF-8; a string that
     * already holds real Unicode, or genuine Latin-1 text, is returned untouched.
     */
    static String fixMetaEncoding(String value) {
        if (value == null) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 0xFF) {
                return value;
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value.getBytes(StandardCharsets.ISO_8859_1)))
                    .toString();
        } catch (CharacterCodingException ex) {
            return value;
        }
    }

    /** X's archive HTML-escapes these in post text. */
    static String unescapeHtml(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    static Instant parseXDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value.trim(), X_DATE).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * A stable id for exports that carry none, so uploading the same archive twice does not
     * create duplicate posts.
     */
    static String fingerprint(SocialProvider provider, Instant postedAt, String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String key = provider.name() + "|" + (postedAt == null ? "" : postedAt.getEpochSecond()) + "|" + text.trim();
            return HexFormat.of().formatHex(digest.digest(key.getBytes(StandardCharsets.UTF_8))).substring(0, 40);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static Instant toInstant(Long epochSeconds) {
        // Instagram writes 0 for some very old posts; that is "unknown", not 1 January 1970.
        return epochSeconds == null || epochSeconds <= 0 ? null : Instant.ofEpochSecond(epochSeconds);
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.isContainerNode()) {
                return value.asText();
            }
        }
        return null;
    }

    private static Long firstLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText().trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isZip(BufferedInputStream in) throws IOException {
        in.mark(4);
        byte[] head = in.readNBytes(4);
        in.reset();
        return head.length == 4 && head[0] == 0x50 && head[1] == 0x4B && head[2] == 0x03 && head[3] == 0x04;
    }

    private byte[] readUpTo(InputStream in, long limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[COPY_BUFFER_BYTES];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw tooLarge();
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private long discard(InputStream in, long budget) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_BYTES];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > budget) {
                throw tooLarge();
            }
        }
        return total;
    }

    private static String displayName(String path) {
        if (path == null || path.isBlank()) {
            return "that file";
        }
        String normalized = path.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private static ApiException tooLarge() {
        return new ApiException("That export is too large to process. Request only your posts, in JSON, "
                + "and without media if the platform offers it.", HttpStatus.PAYLOAD_TOO_LARGE);
    }

    private static ApiException unreadable(SocialProvider provider, String source) {
        String platform = switch (provider) {
            case X -> "an X";
            case INSTAGRAM -> "an Instagram";
            case FACEBOOK -> "a Facebook";
        };
        return new ApiException("We couldn't read " + source + " as " + platform
                + " export. Check you chose the right platform and the JSON format.", HttpStatus.BAD_REQUEST);
    }

    private static String missingPostsMessage(SocialProvider provider) {
        return switch (provider) {
            case X -> "We couldn't find your posts in that archive. It should contain data/tweets.js — "
                    + "upload the ZIP X sent you, or that file on its own.";
            case INSTAGRAM -> "We couldn't find your posts in that archive. Request your Instagram data in JSON "
                    + "format with Posts selected; the file is named posts_1.json.";
            case FACEBOOK -> "We couldn't find your posts in that archive. Request your Facebook data in JSON "
                    + "format with Posts selected; the file is named your_posts_1.json.";
        };
    }

    private static final class Accumulator {
        private final List<ParsedPost> posts = new ArrayList<>();
        private int reposts;
        private int noText;

        void add(ParsedExport export) {
            posts.addAll(export.posts());
            reposts += export.skippedReposts();
            noText += export.skippedNoText();
        }

        ParsedExport result() {
            return new ParsedExport(posts, reposts, noText);
        }
    }
}
