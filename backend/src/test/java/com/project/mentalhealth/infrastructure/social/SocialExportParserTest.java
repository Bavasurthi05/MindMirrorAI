package com.project.mentalhealth.infrastructure.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mentalhealth.application.ports.out.SocialExportParserPort.ParsedExport;
import com.project.mentalhealth.application.ports.out.SocialExportParserPort.ParsedPost;
import com.project.mentalhealth.domain.model.SocialProvider;
import com.project.mentalhealth.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fixtures follow the documented export layouts. Real exports drift, so these pin the
 * shapes we handle rather than guaranteeing every archive a platform ever produced.
 */
class SocialExportParserTest {

    private final SocialExportParser parser = new SocialExportParser(new ObjectMapper(), 5_000_000, 1_000_000);

    private static final String X_TWEETS = """
            window.YTD.tweets.part0 = [
              {"tweet": {"id_str": "1001", "full_text": "Deadlines at work are wearing me down &amp; I can't sleep",
                         "created_at": "Wed Oct 10 20:19:24 +0000 2018"}},
              {"tweet": {"id_str": "1002", "full_text": "RT @friend: someone else's words entirely",
                         "created_at": "Thu Oct 11 08:00:00 +0000 2018"}},
              {"tweet": {"id_str": "1003", "full_text": "@friend honestly feeling a lot calmer today",
                         "created_at": "Fri Oct 12 09:30:00 +0000 2018"}}
            ]
            """;

    private ParsedExport parse(SocialProvider provider, String name, byte[] bytes) {
        return parser.parse(provider, name, new ByteArrayInputStream(bytes));
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(utf8(entry.getValue()));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static List<String> texts(ParsedExport export) {
        return export.posts().stream().map(ParsedPost::text).toList();
    }

    // --- X ---------------------------------------------------------------------------------

    @Test
    void readsXPostsFromTheJavascriptFile() {
        ParsedExport export = parse(SocialProvider.X, "tweets.js", utf8(X_TWEETS));

        assertThat(export.posts()).extracting(ParsedPost::externalId).containsExactly("1001", "1003");
        assertThat(export.posts().get(0).postedAt()).isEqualTo(Instant.parse("2018-10-10T20:19:24Z"));
    }

    @Test
    void repostsAreLeftOutBecauseTheyAreSomeoneElsesWords() {
        ParsedExport export = parse(SocialProvider.X, "tweets.js", utf8(X_TWEETS));

        assertThat(export.skippedReposts()).isEqualTo(1);
        assertThat(texts(export)).noneMatch(text -> text.contains("someone else's"));
    }

    @Test
    void repliesAreTheUsersOwnWordsAndAreKept() {
        assertThat(texts(parse(SocialProvider.X, "tweets.js", utf8(X_TWEETS))))
                .anyMatch(text -> text.contains("feeling a lot calmer"));
    }

    @Test
    void htmlEscapesInXTextAreDecoded() {
        assertThat(texts(parse(SocialProvider.X, "tweets.js", utf8(X_TWEETS))).get(0)).contains("down & I");
    }

    @Test
    void anXArchiveNeverReadsDirectMessagesOrDeletedPosts() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("data/direct-messages.js", """
                window.YTD.direct_messages.part0 = [{"tweet": {"id_str": "9", "full_text": "private DM content here"}}]""");
        entries.put("data/deleted-tweets.js", """
                window.YTD.deleted_tweets.part0 = [{"tweet": {"id_str": "8", "full_text": "a post I deleted myself"}}]""");
        entries.put("data/tweets.js", X_TWEETS);
        entries.put("data/tweets_media/photo.jpg", "binary-ish media bytes");

        ParsedExport export = parse(SocialProvider.X, "twitter-archive.zip", zip(entries));

        assertThat(export.posts()).extracting(ParsedPost::externalId).containsExactly("1001", "1003");
        assertThat(texts(export)).noneMatch(text -> text.contains("DM") || text.contains("deleted"));
    }

    @Test
    void largeXArchivesSplitAcrossPartFilesAreCombined() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("data/tweets-part1.js", """
                window.YTD.tweets.part1 = [{"tweet": {"id_str": "1", "full_text": "first part of a big archive"}}]""");
        entries.put("data/tweets-part2.js", """
                window.YTD.tweets.part2 = [{"tweet": {"id_str": "2", "full_text": "second part of a big archive"}}]""");

        assertThat(parse(SocialProvider.X, "archive.zip", zip(entries)).posts()).hasSize(2);
    }

    @Test
    void olderXArchivesUsingTweetJsAndPlainFieldsStillParse() throws IOException {
        byte[] archive = zip(Map.of("data/tweet.js", """
                window.YTD.tweet.part0 = [{"tweet": {"id": 555, "text": "an older archive format post"}}]"""));

        ParsedExport export = parse(SocialProvider.X, "archive.zip", archive);

        assertThat(export.posts()).extracting(ParsedPost::externalId).containsExactly("555");
    }

    // --- Instagram ---------------------------------------------------------------------------

    @Test
    void singleImageInstagramPostsKeepTheirCaptionAndDateInsideMedia() {
        String json = """
                [{"media": [{"uri": "media/posts/1.jpg", "creation_timestamp": 1700000000,
                             "title": "Feeling grateful for a quiet weekend"}]}]""";

        ParsedExport export = parse(SocialProvider.INSTAGRAM, "posts_1.json", utf8(json));

        assertThat(texts(export)).containsExactly("Feeling grateful for a quiet weekend");
        assertThat(export.posts().get(0).postedAt()).isEqualTo(Instant.ofEpochSecond(1700000000));
    }

    @Test
    void carouselInstagramPostsKeepTheirCaptionAndDateAtTheTop() {
        String json = """
                [{"title": "A carousel from a long day out", "creation_timestamp": 1700000100,
                  "media": [{"uri": "a.jpg", "creation_timestamp": 1, "title": ""},
                            {"uri": "b.jpg", "creation_timestamp": 1, "title": ""}]}]""";

        ParsedExport export = parse(SocialProvider.INSTAGRAM, "posts_1.json", utf8(json));

        assertThat(texts(export)).containsExactly("A carousel from a long day out");
        assertThat(export.posts().get(0).postedAt()).isEqualTo(Instant.ofEpochSecond(1700000100));
    }

    @Test
    void photoOnlyInstagramPostsAreCountedAsHavingNoText() {
        String json = """
                [{"media": [{"uri": "a.jpg", "creation_timestamp": 1700000000, "title": ""}]}]""";

        ParsedExport export = parse(SocialProvider.INSTAGRAM, "posts_1.json", utf8(json));

        assertThat(export.posts()).isEmpty();
        assertThat(export.skippedNoText()).isEqualTo(1);
    }

    @Test
    void metasByteWiseEncodingIsRepairedToRealCharacters() {
        // Meta writes each UTF-8 byte as its own \\u00XX escape: the apostrophe U+2019 arrives as
        // three characters. The double backslash keeps Java from decoding the escape itself.
        String json = """
                [{"media": [{"uri": "a.jpg", "creation_timestamp": 1700000000,
                             "title": "I don\\u00e2\\u0080\\u0099t feel like myself lately"}]}]""";

        assertThat(texts(parse(SocialProvider.INSTAGRAM, "posts_1.json", utf8(json))))
                .containsExactly("I don’t feel like myself lately");
    }

    @Test
    void encodingRepairLeavesAlreadyCorrectTextAlone() {
        assertThat(SocialExportParser.fixMetaEncoding("café au lait")).isEqualTo("café au lait");
        assertThat(SocialExportParser.fixMetaEncoding("naïve – already unicode")).isEqualTo("naïve – already unicode");
        assertThat(SocialExportParser.fixMetaEncoding("plain ascii")).isEqualTo("plain ascii");
    }

    @Test
    void instagramArchivesFindPostsUnderTheCurrentLayout() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("your_instagram_activity/messages/inbox/friend/message_1.json", """
                [{"media": [{"title": "a private message that must not be read"}]}]""");
        entries.put("your_instagram_activity/media/posts_1.json", """
                [{"media": [{"uri": "a.jpg", "creation_timestamp": 1700000000, "title": "My own caption about the week"}]}]""");

        ParsedExport export = parse(SocialProvider.INSTAGRAM, "instagram.zip", zip(entries));

        assertThat(texts(export)).containsExactly("My own caption about the week");
    }

    // --- Facebook ----------------------------------------------------------------------------

    @Test
    void facebookPostsJoinTheirTextParts() {
        String json = """
                [{"timestamp": 1690000000, "title": "Ada updated her status.",
                  "data": [{"post": "Long day but I got through it"}, {"update_timestamp": 1690000000}]},
                 {"timestamp": 1690000100, "attachments": [{"data": [{"media": {"uri": "p.jpg"}}]}], "data": []}]""";

        ParsedExport export = parse(SocialProvider.FACEBOOK, "your_posts_1.json", utf8(json));

        assertThat(texts(export)).containsExactly("Long day but I got through it");
        assertThat(export.skippedNoText()).isEqualTo(1);
        assertThat(export.posts().get(0).postedAt()).isEqualTo(Instant.ofEpochSecond(1690000000));
    }

    @Test
    void olderFacebookExportsWrappedInStatusUpdatesStillParse() {
        String json = """
                {"status_updates": [{"timestamp": 1415339550, "data": [{"post": "An older style status update"}]}]}""";

        assertThat(texts(parse(SocialProvider.FACEBOOK, "your_posts.json", utf8(json))))
                .containsExactly("An older style status update");
    }

    @Test
    void facebookArchivesIgnoreMessages() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("your_facebook_activity/messages/inbox/friend_1/message_1.json", """
                {"messages": [{"data": [{"post": "a private chat message"}]}]}""");
        entries.put("your_facebook_activity/posts/your_posts__check_ins__photos_and_videos_1.json", """
                [{"timestamp": 1690000000, "data": [{"post": "My own post on my own timeline"}]}]""");

        assertThat(texts(parse(SocialProvider.FACEBOOK, "facebook.zip", zip(entries))))
                .containsExactly("My own post on my own timeline");
    }

    // --- Stable ids ---------------------------------------------------------------------------

    @Test
    void exportsWithoutIdsGetTheSameIdEachTimeSoReUploadsDeduplicate() {
        String json = """
                [{"media": [{"uri": "a.jpg", "creation_timestamp": 1700000000, "title": "The same caption twice"}]}]""";

        String first = parse(SocialProvider.INSTAGRAM, "posts_1.json", utf8(json)).posts().get(0).externalId();
        String second = parse(SocialProvider.INSTAGRAM, "posts_1.json", utf8(json)).posts().get(0).externalId();

        assertThat(first).isEqualTo(second).hasSize(40);
    }

    // --- Refusals ----------------------------------------------------------------------------

    @Test
    void anArchiveWithoutAPostsFileExplainsWhatWasExpected() throws IOException {
        byte[] archive = zip(Map.of("data/account.js", "window.YTD.account.part0 = []"));

        assertThatThrownBy(() -> parse(SocialProvider.X, "archive.zip", archive))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("tweets.js");
    }

    @Test
    void unreadableContentIsRejected() {
        assertThatThrownBy(() -> parse(SocialProvider.INSTAGRAM, "posts_1.json", utf8("this is not json {")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("couldn't read");
    }

    @Test
    void aZipBombIsStoppedByTheInflatedByteLimit() throws IOException {
        SocialExportParser strict = new SocialExportParser(new ObjectMapper(), 50_000, 1_000_000);
        Map<String, String> entries = new LinkedHashMap<>();
        // Compresses to almost nothing but inflates past the budget.
        entries.put("data/tweets_media/huge.bin", "a".repeat(500_000));
        entries.put("data/tweets.js", X_TWEETS);
        byte[] archive = zip(entries);

        assertThatThrownBy(() -> strict.parse(SocialProvider.X, "archive.zip", new ByteArrayInputStream(archive)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void anOversizedPostsFileIsRefused() {
        SocialExportParser strict = new SocialExportParser(new ObjectMapper(), 5_000_000, 100);

        assertThatThrownBy(() -> strict.parse(SocialProvider.X, "tweets.js", new ByteArrayInputStream(utf8(X_TWEETS))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    // --- Helpers -----------------------------------------------------------------------------

    @Test
    void postsFilesAreRecognisedByNameAndMessagesNeverAre() {
        assertThat(SocialExportParser.isPostsFile(SocialProvider.X, "data/tweets.js")).isTrue();
        assertThat(SocialExportParser.isPostsFile(SocialProvider.X, "data/tweets-part3.js")).isTrue();
        assertThat(SocialExportParser.isPostsFile(SocialProvider.X, "data/tweet-headers.js")).isFalse();
        assertThat(SocialExportParser.isPostsFile(SocialProvider.X, "data/direct-messages.js")).isFalse();
        assertThat(SocialExportParser.isPostsFile(SocialProvider.X, "data/deleted-tweets.js")).isFalse();
        assertThat(SocialExportParser.isPostsFile(SocialProvider.INSTAGRAM, "your_instagram_activity/media/posts_1.json")).isTrue();
        assertThat(SocialExportParser.isPostsFile(SocialProvider.INSTAGRAM, "messages/inbox/a/posts_1.json")).isFalse();
        assertThat(SocialExportParser.isPostsFile(SocialProvider.FACEBOOK, "posts/your_posts_1.json")).isTrue();
        assertThat(SocialExportParser.isPostsFile(SocialProvider.FACEBOOK, "posts/other_peoples_posts_1.json")).isFalse();
    }

    @Test
    void theJavascriptWrapperIsStripped() {
        assertThat(SocialExportParser.stripJavascriptAssignment("window.YTD.tweets.part0 = [1]")).isEqualTo(" [1]");
        assertThat(SocialExportParser.stripJavascriptAssignment("[1]")).isEqualTo("[1]");
    }

    @Test
    void anUnparseableXDateBecomesUnknownRatherThanFailing() {
        assertThat(SocialExportParser.parseXDate("not a date")).isNull();
        assertThat(SocialExportParser.parseXDate(null)).isNull();
    }
}
