package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.out.SocialExportParserPort;
import com.project.mentalhealth.application.ports.out.SocialExportParserPort.ParsedExport;
import com.project.mentalhealth.application.ports.out.SocialExportParserPort.ParsedPost;
import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.model.SocialImport;
import com.project.mentalhealth.domain.model.SocialImportStatus;
import com.project.mentalhealth.domain.model.SocialPost;
import com.project.mentalhealth.domain.model.SocialProvider;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.SocialImportRepository;
import com.project.mentalhealth.domain.repository.SocialPostRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.socialimport.dto.SocialImportResponse;
import com.project.mentalhealth.interfaces.api.v1.socialimport.dto.SocialInsightsResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialImportServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SocialImportRepository socialImportRepository;
    @Mock private SocialPostRepository socialPostRepository;
    @Mock private AnalysisResultRepository analysisRepository;
    @Mock private SocialExportParserPort exportParser;
    @Mock private AnalysisOrchestrator analysisOrchestrator;
    @Mock private UserPreferencesService preferencesService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ObjectProvider<SocialImportService> self;

    private SocialImportService service;
    private User user;

    @BeforeEach
    void setUp() {
        // max 3 posts per import keeps the cap test small.
        service = new SocialImportService(userRepository, socialImportRepository, socialPostRepository,
                analysisRepository, exportParser, analysisOrchestrator, preferencesService, eventPublisher,
                self, 3, 15, 1_000_000);
        given(self.getObject()).willReturn(service);

        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(preferencesService.zoneFor(anyLong())).willReturn(ZoneOffset.UTC);

        given(socialImportRepository.save(any(SocialImport.class))).willAnswer(invocation -> {
            SocialImport saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(10L);
            }
            return saved;
        });
        given(socialPostRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(socialPostRepository.findExternalPostIds(anyLong(), any())).willReturn(List.of());
        given(socialPostRepository.countAnalysisStatuses(anyLong())).willReturn(List.of());
    }

    private static MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "application/octet-stream", "export-bytes".getBytes());
    }

    private static ParsedPost post(String id, String text, String isoDate) {
        return new ParsedPost(id, text, isoDate == null ? null : Instant.parse(isoDate));
    }

    private void parserReturns(ParsedExport export) {
        given(exportParser.parse(any(), any(), any())).willReturn(export);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<SocialPost> savedPosts() {
        ArgumentCaptor<List<SocialPost>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(socialPostRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // --- Upload validation ----------------------------------------------------------------

    @Test
    void uploadingRequiresTheUserToAcknowledgeHowPostsAreUsed() {
        assertThatThrownBy(() -> service.importExport("user@example.com", "x", file("tweets.js"), false))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("confirm");
        verify(exportParser, never()).parse(any(), any(), any());
    }

    @Test
    void anUnknownPlatformIsRejected() {
        assertThatThrownBy(() -> service.importExport("user@example.com", "myspace", file("posts.json"), true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("X, Instagram or Facebook");
    }

    @Test
    void anEmptyFileIsRejected() {
        MockMultipartFile empty = new MockMultipartFile("file", "tweets.js", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.importExport("user@example.com", "x", empty, true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Choose a file");
    }

    @Test
    void aFileThatCannotBeAnExportIsRejectedBeforeParsing() {
        assertThatThrownBy(() -> service.importExport("user@example.com", "x", file("notes.txt"), true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(".zip");
        verify(exportParser, never()).parse(any(), any(), any());
    }

    @Test
    void aFileWithNoPostsAtAllIsRejected() {
        parserReturns(new ParsedExport(List.of(), 0, 0));

        assertThatThrownBy(() -> service.importExport("user@example.com", "x", file("tweets.js"), true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("couldn't find any posts");
    }

    // --- Import ---------------------------------------------------------------------------

    @Test
    void postsAreSavedAndAnalysisIsQueued() {
        parserReturns(new ParsedExport(List.of(
                post("a", "A long enough post about my week", "2026-01-02T10:00:00Z"),
                post("b", "Another long enough post about work", "2026-01-03T10:00:00Z")), 0, 0));

        SocialImportResponse response = service.importExport("user@example.com", "twitter", file("tweets.js"), true);

        assertThat(response.getProvider()).isEqualTo("X");
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getPostsImported()).isEqualTo(2);
        assertThat(savedPosts()).extracting(SocialPost::getExternalPostId).containsExactlyInAnyOrder("a", "b");
        verify(eventPublisher).publishEvent(new SocialImportRequestedEvent(10L));
    }

    @Test
    void theUploadRecordsWhenItWasAcknowledged() {
        parserReturns(new ParsedExport(List.of(post("a", "A long enough post about my week", null)), 0, 0));

        service.importExport("user@example.com", "x", file("tweets.js"), true);

        ArgumentCaptor<SocialImport> captor = ArgumentCaptor.forClass(SocialImport.class);
        verify(socialImportRepository).save(captor.capture());
        assertThat(captor.getValue().getAcknowledgedAt()).isNotNull();
    }

    @Test
    void shortPostsAreSkippedAndCounted() {
        parserReturns(new ParsedExport(List.of(
                post("a", "lol", "2026-01-02T10:00:00Z"),
                post("b", "A long enough post about my week", "2026-01-03T10:00:00Z")), 0, 2));

        SocialImportResponse response = service.importExport("user@example.com", "x", file("tweets.js"), true);

        // Two photo-only posts from the parser, plus "lol".
        assertThat(response.getSkippedShort()).isEqualTo(3);
        assertThat(response.getPostsImported()).isEqualTo(1);
    }

    @Test
    void repostCountsFromTheParserAreReported() {
        parserReturns(new ParsedExport(List.of(post("a", "A long enough post about my week", null)), 4, 0));

        SocialImportResponse response = service.importExport("user@example.com", "x", file("tweets.js"), true);

        assertThat(response.getSkippedReposts()).isEqualTo(4);
        assertThat(response.getPostsFound()).isEqualTo(5);
    }

    @Test
    void postsImportedEarlierAreNotImportedAgain() {
        given(socialPostRepository.findExternalPostIds(1L, SocialProvider.X)).willReturn(List.of("a"));
        parserReturns(new ParsedExport(List.of(
                post("a", "A long enough post about my week", "2026-01-02T10:00:00Z"),
                post("b", "Another long enough post about work", "2026-01-03T10:00:00Z")), 0, 0));

        SocialImportResponse response = service.importExport("user@example.com", "x", file("tweets.js"), true);

        assertThat(response.getSkippedDuplicates()).isEqualTo(1);
        assertThat(savedPosts()).extracting(SocialPost::getExternalPostId).containsExactly("b");
    }

    @Test
    void duplicatesWithinOneFileAreImportedOnce() {
        parserReturns(new ParsedExport(List.of(
                post("a", "A long enough post about my week", "2026-01-02T10:00:00Z"),
                post("a", "A long enough post about my week", "2026-01-02T10:00:00Z")), 0, 0));

        SocialImportResponse response = service.importExport("user@example.com", "x", file("tweets.js"), true);

        assertThat(response.getSkippedDuplicates()).isEqualTo(1);
        assertThat(savedPosts()).hasSize(1);
    }

    @Test
    void overTheLimitTheMostRecentPostsAreKept() {
        parserReturns(new ParsedExport(List.of(
                post("oldest", "A long enough post from long ago", "2020-01-01T00:00:00Z"),
                post("newest", "A long enough post from this week", "2026-03-01T00:00:00Z"),
                post("undated", "A long enough post with no date", null),
                post("middle", "A long enough post from last year", "2025-06-01T00:00:00Z"),
                post("recent", "A long enough post from last month", "2026-02-01T00:00:00Z")), 0, 0));

        SocialImportResponse response = service.importExport("user@example.com", "x", file("tweets.js"), true);

        assertThat(response.getSkippedOverLimit()).isEqualTo(2);
        assertThat(savedPosts()).extracting(SocialPost::getExternalPostId)
                .containsExactly("newest", "recent", "middle");
    }

    @Test
    void anUploadWithNothingNewCompletesWithoutQueuingAnalysis() {
        given(socialPostRepository.findExternalPostIds(1L, SocialProvider.X)).willReturn(List.of("a"));
        parserReturns(new ParsedExport(List.of(post("a", "A long enough post about my week", null)), 0, 0));

        SocialImportResponse response = service.importExport("user@example.com", "x", file("tweets.js"), true);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- Background analysis ----------------------------------------------------------------

    private SocialPost storedPost(long id) {
        SocialPost post = new SocialPost();
        post.setId(id);
        post.setUser(user);
        post.setProvider(SocialProvider.X);
        post.setContent("post number " + id + " with enough text");
        return post;
    }

    @Test
    void postsAreAnalyzedInBatchesOfFiftyAsSocialSource() {
        List<SocialPost> pending = LongStream.rangeClosed(1, 120).mapToObj(this::storedPost).toList();
        given(socialPostRepository.findBySocialImportIdAndAnalysisResultIdIsNullOrderByIdAsc(10L)).willReturn(pending);
        given(socialPostRepository.findAllById(any())).willAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<SocialPost> found = new ArrayList<>();
            ids.forEach(id -> found.add(storedPost(id)));
            return found;
        });
        given(analysisOrchestrator.recordBatch(any(), any(), any())).willReturn(List.of());
        SocialImport socialImport = new SocialImport();
        given(socialImportRepository.findById(10L)).willReturn(Optional.of(socialImport));

        service.analyzeImport(10L);

        verify(analysisOrchestrator, times(3)).recordBatch(any(), eq(AnalysisSourceType.SOCIAL), any());
        assertThat(socialImport.getStatus()).isEqualTo(SocialImportStatus.COMPLETED);
    }

    @Test
    void oneFailedBatchDoesNotStrandTheRest() {
        List<SocialPost> pending = LongStream.rangeClosed(1, 60).mapToObj(this::storedPost).toList();
        given(socialPostRepository.findBySocialImportIdAndAnalysisResultIdIsNullOrderByIdAsc(10L)).willReturn(pending);
        given(socialPostRepository.findAllById(any())).willAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<SocialPost> found = new ArrayList<>();
            ids.forEach(id -> found.add(storedPost(id)));
            return found;
        });
        given(analysisOrchestrator.recordBatch(any(), any(), any()))
                .willThrow(new RuntimeException("database hiccup"))
                .willReturn(List.of());
        SocialImport socialImport = new SocialImport();
        given(socialImportRepository.findById(10L)).willReturn(Optional.of(socialImport));

        service.analyzeImport(10L);

        verify(analysisOrchestrator, times(2)).recordBatch(any(), any(), any());
        assertThat(socialImport.getStatus()).isEqualTo(SocialImportStatus.COMPLETED);
    }

    @Test
    void analysesAreLinkedBackToTheirPosts() {
        SocialPost post = storedPost(5);
        given(socialPostRepository.findAllById(any())).willReturn(List.of(post));
        AnalysisResult result = new AnalysisResult();
        result.setId(77L);
        result.setSourceId(5L);
        given(analysisOrchestrator.recordBatch(any(), any(), any())).willReturn(List.of(result));

        service.analyzeChunk(List.of(5L));

        assertThat(post.getAnalysisResultId()).isEqualTo(77L);
    }

    @Test
    void alreadyAnalyzedPostsAreNotSentAgain() {
        SocialPost done = storedPost(5);
        done.setAnalysisResultId(99L);
        given(socialPostRepository.findAllById(any())).willReturn(List.of(done));

        service.analyzeChunk(List.of(5L));

        verify(analysisOrchestrator, never()).recordBatch(any(), any(), any());
    }

    // --- Deletion ---------------------------------------------------------------------------

    @Test
    void deletingAnImportRemovesItsPostsAndTheirAnalyses() {
        SocialImport socialImport = new SocialImport();
        socialImport.setId(10L);
        given(socialImportRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(socialImport));
        SocialPost analyzed = storedPost(1);
        analyzed.setAnalysisResultId(5L);
        SocialPost unanalyzed = storedPost(2);
        List<SocialPost> posts = List.of(analyzed, unanalyzed);
        given(socialPostRepository.findBySocialImportId(10L)).willReturn(posts);

        service.delete("user@example.com", 10L);

        verify(socialPostRepository).deleteAll(posts);
        verify(analysisRepository).deleteAllById(List.of(5L));
        verify(socialImportRepository).delete(socialImport);
    }

    @Test
    void someoneElsesImportCannotBeDeleted() {
        given(socialImportRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("user@example.com", 10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
        verify(socialPostRepository, never()).deleteAll(any());
        verify(analysisRepository, never()).deleteAllById(any());
    }

    // --- Insights ----------------------------------------------------------------------------

    private SocialPost postedAt(long id, String isoDate, Long analysisId) {
        SocialPost post = storedPost(id);
        post.setPostedAt(isoDate == null ? null : Instant.parse(isoDate));
        post.setAnalysisResultId(analysisId);
        return post;
    }

    private AnalysisResult analysis(long id, double sentiment, String prediction, String emotion) {
        AnalysisResult result = new AnalysisResult();
        result.setId(id);
        result.setStatus(AnalysisStatus.OK);
        result.setSentimentScore(sentiment);
        result.setPrediction(prediction);
        result.setDominantEmotion(emotion);
        return result;
    }

    @Test
    void insightsGroupByTheMonthPostsWerePublishedNotWhenTheyWereAnalyzed() {
        given(socialPostRepository.findByUserIdOrderByPostedAtDesc(1L)).willReturn(List.of(
                postedAt(3, "2026-02-10T10:00:00Z", 103L),
                postedAt(2, "2026-01-20T10:00:00Z", 102L),
                postedAt(1, "2026-01-05T10:00:00Z", 101L),
                postedAt(4, null, 104L),
                postedAt(5, "2026-02-11T10:00:00Z", null)));
        given(analysisRepository.findAllById(any())).willReturn(List.of(
                analysis(101, 0.5, "normal", "joy"),
                analysis(102, -0.5, "anxiety", "fear"),
                analysis(103, -0.2, "stress", "fear"),
                analysis(104, 0.1, "normal", "neutral")));

        SocialInsightsResponse insights = service.insights("user@example.com");

        assertThat(insights.getTotalPosts()).isEqualTo(5);
        assertThat(insights.getAnalyzedPosts()).isEqualTo(4);
        assertThat(insights.getPendingPosts()).isEqualTo(1);
        assertThat(insights.getUndatedPosts()).isEqualTo(1);
        assertThat(insights.getTimeline()).extracting(SocialInsightsResponse.MonthPoint::getLabel)
                .containsExactly("Jan 2026", "Feb 2026");
        SocialInsightsResponse.MonthPoint january = insights.getTimeline().get(0);
        assertThat(january.getPosts()).isEqualTo(2);
        assertThat(january.getAverageSentiment()).isEqualTo(0.0);
        assertThat(january.getConcernShare()).isEqualTo(0.5);
        assertThat(insights.getPredictionDistribution()).containsEntry("normal", 2L).containsEntry("anxiety", 1L);
        assertThat(insights.getTopEmotions().get(0).getEmotion()).isEqualTo("fear");
    }

    @Test
    void failedAnalysesDoNotCountAsInsights() {
        given(socialPostRepository.findByUserIdOrderByPostedAtDesc(1L))
                .willReturn(List.of(postedAt(1, "2026-01-05T10:00:00Z", 101L)));
        AnalysisResult failed = analysis(101, 0, null, null);
        failed.setStatus(AnalysisStatus.FAILED);
        given(analysisRepository.findAllById(any())).willReturn(List.of(failed));

        SocialInsightsResponse insights = service.insights("user@example.com");

        assertThat(insights.getAnalyzedPosts()).isZero();
        assertThat(insights.getAverageSentiment()).isNull();
        assertThat(insights.getTimeline()).isEmpty();
    }
}
