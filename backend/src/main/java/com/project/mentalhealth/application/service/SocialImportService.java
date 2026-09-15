package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.application.ports.out.SocialExportParserPort;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Imports posts from a user's own social media data export and analyzes them.
 *
 * <p>Parsing happens during the upload request, because the file only exists for that request.
 * Analysis happens afterwards in batches, so the upload returns as soon as posts are saved.
 *
 * <p>Imported posts are analyzed but deliberately kept out of the day-to-day signals: they
 * derive no mood or trigger entries and do not feed the dashboard, mindset score or baseline.
 * An archive spanning years is analyzed in one go, and mixing it in would bury what the user
 * logged this week under hundreds of entries all dated today.
 */
@Service
public class SocialImportService {

    private static final Logger log = LoggerFactory.getLogger(SocialImportService.class);

    private static final Set<String> ACCEPTED_EXTENSIONS = Set.of(".zip", ".json", ".js");
    private static final Set<String> CONCERN_PREDICTIONS = Set.of("stress", "anxiety", "depression");
    private static final int TIMELINE_MONTHS = 24;
    private static final int RECENT_POSTS = 20;
    private static final int EXCERPT_LENGTH = 280;
    private static final int TOP_EMOTIONS = 6;
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final SocialImportRepository socialImportRepository;
    private final SocialPostRepository socialPostRepository;
    private final AnalysisResultRepository analysisRepository;
    private final SocialExportParserPort exportParser;
    private final AnalysisOrchestrator analysisOrchestrator;
    private final UserPreferencesService preferencesService;
    private final ApplicationEventPublisher eventPublisher;
    /** Self-reference so per-chunk transactions go through the proxy. */
    private final ObjectProvider<SocialImportService> self;
    private final int maxPosts;
    private final int minTextLength;
    private final long maxUploadBytes;

    public SocialImportService(UserRepository userRepository,
                               SocialImportRepository socialImportRepository,
                               SocialPostRepository socialPostRepository,
                               AnalysisResultRepository analysisRepository,
                               SocialExportParserPort exportParser,
                               AnalysisOrchestrator analysisOrchestrator,
                               UserPreferencesService preferencesService,
                               ApplicationEventPublisher eventPublisher,
                               ObjectProvider<SocialImportService> self,
                               @Value("${app.social-import.max-posts:1000}") int maxPosts,
                               @Value("${app.social-import.min-text-length:15}") int minTextLength,
                               @Value("${app.social-import.max-upload-bytes:104857600}") long maxUploadBytes) {
        this.userRepository = userRepository;
        this.socialImportRepository = socialImportRepository;
        this.socialPostRepository = socialPostRepository;
        this.analysisRepository = analysisRepository;
        this.exportParser = exportParser;
        this.analysisOrchestrator = analysisOrchestrator;
        this.preferencesService = preferencesService;
        this.eventPublisher = eventPublisher;
        this.self = self;
        this.maxPosts = maxPosts;
        this.minTextLength = minTextLength;
        this.maxUploadBytes = maxUploadBytes;
    }

    // --- Upload ---------------------------------------------------------------------------

    /**
     * Parse an uploaded export and queue its posts for analysis.
     *
     * <p>The raw file is never stored: only the text and date of each of the user's own posts
     * are kept, and the upload is discarded when the request ends.
     */
    @Transactional
    public SocialImportResponse importExport(String userEmail, String providerParam, MultipartFile file,
                                             boolean acknowledged) {
        if (!acknowledged) {
            throw new ApiException("Please confirm you understand how your posts will be used before uploading.",
                    HttpStatus.BAD_REQUEST);
        }
        SocialProvider provider = SocialProvider.fromParam(providerParam)
                .orElseThrow(() -> new ApiException("Choose X, Instagram or Facebook.", HttpStatus.BAD_REQUEST));
        String filename = validateFile(file);
        User user = requireUser(userEmail);

        SocialExportParserPort.ParsedExport parsed;
        try (InputStream content = file.getInputStream()) {
            parsed = exportParser.parse(provider, filename, content);
        } catch (IOException ex) {
            throw new ApiException("We couldn't read that file. Please try again.", HttpStatus.BAD_REQUEST);
        }

        int found = parsed.posts().size() + parsed.skippedReposts() + parsed.skippedNoText();
        if (found == 0) {
            throw new ApiException("We couldn't find any posts in that file. Check it is the posts file from your "
                    + label(provider) + " export.", HttpStatus.BAD_REQUEST);
        }

        int skippedShort = parsed.skippedNoText();
        List<SocialExportParserPort.ParsedPost> usable = new ArrayList<>();
        for (SocialExportParserPort.ParsedPost post : parsed.posts()) {
            if (post.text().trim().length() < minTextLength) {
                skippedShort++;
            } else {
                usable.add(post);
            }
        }

        // Duplicates against earlier uploads, and within this file (archives can repeat posts
        // across part files).
        Set<String> known = new HashSet<>(socialPostRepository.findExternalPostIds(user.getId(), provider));
        Map<String, SocialExportParserPort.ParsedPost> unique = new LinkedHashMap<>();
        int skippedDuplicates = 0;
        for (SocialExportParserPort.ParsedPost post : usable) {
            if (known.contains(post.externalId()) || unique.containsKey(post.externalId())) {
                skippedDuplicates++;
            } else {
                unique.put(post.externalId(), post);
            }
        }

        // Most recent first, so a capped import keeps what reflects the user now.
        List<SocialExportParserPort.ParsedPost> ordered = new ArrayList<>(unique.values());
        ordered.sort(Comparator.comparing(SocialExportParserPort.ParsedPost::postedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<SocialExportParserPort.ParsedPost> kept = ordered.size() > maxPosts ? ordered.subList(0, maxPosts) : ordered;

        SocialImport socialImport = new SocialImport();
        socialImport.setUser(user);
        socialImport.setProvider(provider);
        socialImport.setOriginalFilename(truncate(filename, 255));
        socialImport.setPostsFound(found);
        socialImport.setPostsImported(kept.size());
        socialImport.setSkippedDuplicates(skippedDuplicates);
        socialImport.setSkippedReposts(parsed.skippedReposts());
        socialImport.setSkippedShort(skippedShort);
        socialImport.setSkippedOverLimit(ordered.size() - kept.size());
        socialImport.setAcknowledgedAt(Instant.now());
        kept.stream().map(SocialExportParserPort.ParsedPost::postedAt).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).ifPresent(socialImport::setEarliestPostAt);
        kept.stream().map(SocialExportParserPort.ParsedPost::postedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).ifPresent(socialImport::setLatestPostAt);

        if (kept.isEmpty()) {
            // Everything was a duplicate or too short: nothing to analyze, and nothing failed.
            socialImport.setStatus(SocialImportStatus.COMPLETED);
            socialImport.setCompletedAt(Instant.now());
        } else {
            socialImport.setStatus(SocialImportStatus.PROCESSING);
        }
        SocialImport saved = socialImportRepository.save(socialImport);

        List<SocialPost> posts = kept.stream().map(parsedPost -> {
            SocialPost post = new SocialPost();
            post.setUser(user);
            post.setSocialImport(saved);
            post.setProvider(provider);
            post.setExternalPostId(parsedPost.externalId());
            post.setContent(parsedPost.text());
            post.setPostedAt(parsedPost.postedAt());
            return post;
        }).toList();
        socialPostRepository.saveAll(posts);

        if (!kept.isEmpty()) {
            eventPublisher.publishEvent(new SocialImportRequestedEvent(saved.getId()));
        }
        log.info("User {} imported {} {} posts ({} found, {} duplicates, {} reposts, {} short, {} over limit)",
                user.getId(), kept.size(), provider, found, skippedDuplicates, parsed.skippedReposts(),
                skippedShort, saved.getSkippedOverLimit());
        return toResponse(saved);
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Choose a file to upload.", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > maxUploadBytes) {
            throw new ApiException("That file is too large to upload. Request only your posts, in JSON, "
                    + "and without media if the platform offers it.", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = filename.toLowerCase(Locale.ROOT);
        if (ACCEPTED_EXTENSIONS.stream().noneMatch(lower::endsWith)) {
            throw new ApiException("Upload the .zip export, or the .json / .js posts file from inside it.",
                    HttpStatus.BAD_REQUEST);
        }
        return filename;
    }

    // --- Background analysis --------------------------------------------------------------

    /**
     * Analyze an import's posts in ML-sized batches, each in its own transaction, so progress is
     * visible as it happens and one failed batch does not undo the others.
     */
    public void analyzeImport(Long importId) {
        List<Long> postIds = self.getObject().unanalyzedPostIds(importId);
        int batchSize = MlAnalysisPort.MAX_BATCH_SIZE;
        for (int start = 0; start < postIds.size(); start += batchSize) {
            List<Long> chunk = postIds.subList(start, Math.min(start + batchSize, postIds.size()));
            try {
                self.getObject().analyzeChunk(chunk);
            } catch (Exception ex) {
                log.warn("Social import {} batch starting at {} failed: {}", importId, start, ex.getMessage());
            }
        }
        self.getObject().finishImport(importId);
    }

    @Transactional(readOnly = true)
    public List<Long> unanalyzedPostIds(Long importId) {
        return socialPostRepository.findBySocialImportIdAndAnalysisResultIdIsNullOrderByIdAsc(importId).stream()
                .map(SocialPost::getId)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void analyzeChunk(List<Long> postIds) {
        List<SocialPost> posts = socialPostRepository.findAllById(postIds).stream()
                .filter(post -> post.getAnalysisResultId() == null)
                .toList();
        if (posts.isEmpty()) {
            // The import was deleted mid-way, or these posts were already analyzed.
            return;
        }
        List<AnalysisResult> results = analysisOrchestrator.recordBatch(
                posts.get(0).getUser(),
                AnalysisSourceType.SOCIAL,
                posts.stream().map(post -> new AnalysisOrchestrator.BatchItem(post.getId(), post.getContent())).toList());

        Map<Long, Long> analysisByPost = results.stream()
                .collect(Collectors.toMap(AnalysisResult::getSourceId, AnalysisResult::getId, (first, second) -> first));
        for (SocialPost post : posts) {
            post.setAnalysisResultId(analysisByPost.get(post.getId()));
        }
        socialPostRepository.saveAll(posts);
    }

    @Transactional
    public void finishImport(Long importId) {
        socialImportRepository.findById(importId).ifPresent(socialImport -> {
            socialImport.setStatus(SocialImportStatus.COMPLETED);
            socialImport.setCompletedAt(Instant.now());
            socialImportRepository.save(socialImport);
        });
    }

    // --- Reads and deletion ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SocialImportResponse> list(String userEmail) {
        User user = requireUser(userEmail);
        return socialImportRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SocialImportResponse get(String userEmail, Long importId) {
        User user = requireUser(userEmail);
        return toResponse(requireImport(importId, user.getId()));
    }

    /**
     * Remove an import, its posts and their analyses — the user's way to take their data back.
     */
    @Transactional
    public void delete(String userEmail, Long importId) {
        User user = requireUser(userEmail);
        SocialImport socialImport = requireImport(importId, user.getId());

        List<SocialPost> posts = socialPostRepository.findBySocialImportId(importId);
        List<Long> analysisIds = posts.stream()
                .map(SocialPost::getAnalysisResultId)
                .filter(Objects::nonNull)
                .toList();

        socialPostRepository.deleteAll(posts);
        analysisRepository.deleteAllById(analysisIds);
        socialImportRepository.delete(socialImport);
        log.info("User {} deleted social import {} ({} posts, {} analyses)",
                user.getId(), importId, posts.size(), analysisIds.size());
    }

    /** What the user's imported posts say, grouped by when they were posted. */
    @Transactional(readOnly = true)
    public SocialInsightsResponse insights(String userEmail) {
        User user = requireUser(userEmail);
        ZoneId zone = preferencesService.zoneFor(user.getId());
        List<SocialPost> posts = socialPostRepository.findByUserIdOrderByPostedAtDesc(user.getId());

        List<Long> analysisIds = posts.stream().map(SocialPost::getAnalysisResultId).filter(Objects::nonNull).toList();
        Map<Long, AnalysisResult> analyses = analysisRepository.findAllById(analysisIds).stream()
                .filter(analysis -> analysis.getStatus() == AnalysisStatus.OK)
                .collect(Collectors.toMap(AnalysisResult::getId, analysis -> analysis));

        List<SocialPost> analyzed = posts.stream()
                .filter(post -> post.getAnalysisResultId() != null && analyses.containsKey(post.getAnalysisResultId()))
                .toList();

        Map<String, Long> byProvider = new TreeMap<>(posts.stream()
                .collect(Collectors.groupingBy(post -> post.getProvider().name(), Collectors.counting())));

        Map<String, Long> predictions = new TreeMap<>();
        Map<String, Long> emotions = new LinkedHashMap<>();
        double sentimentTotal = 0;
        int sentimentCount = 0;
        for (SocialPost post : analyzed) {
            AnalysisResult analysis = analyses.get(post.getAnalysisResultId());
            if (analysis.getPrediction() != null) {
                predictions.merge(analysis.getPrediction(), 1L, Long::sum);
            }
            if (analysis.getDominantEmotion() != null) {
                emotions.merge(analysis.getDominantEmotion(), 1L, Long::sum);
            }
            if (analysis.getSentimentScore() != null) {
                sentimentTotal += analysis.getSentimentScore();
                sentimentCount++;
            }
        }

        return SocialInsightsResponse.builder()
                .totalPosts(posts.size())
                .analyzedPosts(analyzed.size())
                .pendingPosts(posts.size() - analyzed.size())
                .undatedPosts((int) analyzed.stream().filter(post -> post.getPostedAt() == null).count())
                .postsByProvider(byProvider)
                .predictionDistribution(predictions)
                .averageSentiment(sentimentCount == 0 ? null : round(sentimentTotal / sentimentCount))
                .topEmotions(emotions.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(TOP_EMOTIONS)
                        .map(entry -> SocialInsightsResponse.EmotionCount.builder()
                                .emotion(entry.getKey())
                                .count(entry.getValue())
                                .build())
                        .toList())
                .timeline(buildTimeline(analyzed, analyses, zone))
                .recentPosts(analyzed.stream()
                        .limit(RECENT_POSTS)
                        .map(post -> toPostInsight(post, analyses.get(post.getAnalysisResultId())))
                        .toList())
                .build();
    }

    /** Monthly averages by posting date, in the user's timezone, for the most recent months. */
    private List<SocialInsightsResponse.MonthPoint> buildTimeline(List<SocialPost> analyzed,
                                                                  Map<Long, AnalysisResult> analyses, ZoneId zone) {
        Map<YearMonth, List<AnalysisResult>> byMonth = new TreeMap<>();
        for (SocialPost post : analyzed) {
            if (post.getPostedAt() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(post.getPostedAt().atZone(zone));
            byMonth.computeIfAbsent(month, key -> new ArrayList<>()).add(analyses.get(post.getAnalysisResultId()));
        }

        List<SocialInsightsResponse.MonthPoint> points = new ArrayList<>();
        for (Map.Entry<YearMonth, List<AnalysisResult>> entry : byMonth.entrySet()) {
            List<AnalysisResult> monthAnalyses = entry.getValue();
            double sentiment = monthAnalyses.stream()
                    .map(AnalysisResult::getSentimentScore).filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue).average().orElse(0);
            long concerning = monthAnalyses.stream()
                    .filter(analysis -> analysis.getPrediction() != null
                            && CONCERN_PREDICTIONS.contains(analysis.getPrediction()))
                    .count();
            points.add(SocialInsightsResponse.MonthPoint.builder()
                    .month(entry.getKey().toString())
                    .label(entry.getKey().format(MONTH_LABEL))
                    .posts(monthAnalyses.size())
                    .averageSentiment(round(sentiment))
                    .concernShare(round((double) concerning / monthAnalyses.size()))
                    .build());
        }
        return points.size() > TIMELINE_MONTHS ? points.subList(points.size() - TIMELINE_MONTHS, points.size()) : points;
    }

    private SocialInsightsResponse.PostInsight toPostInsight(SocialPost post, AnalysisResult analysis) {
        String content = post.getContent();
        return SocialInsightsResponse.PostInsight.builder()
                .id(post.getId())
                .analysisId(analysis.getId())
                .provider(post.getProvider().name())
                .postedAt(post.getPostedAt())
                .excerpt(content.length() <= EXCERPT_LENGTH ? content : content.substring(0, EXCERPT_LENGTH) + "…")
                .sentiment(analysis.getSentiment())
                .sentimentScore(analysis.getSentimentScore())
                .prediction(analysis.getPrediction())
                .predictionConfidence(analysis.getPredictionConfidence())
                .dominantEmotion(analysis.getDominantEmotion())
                .build();
    }

    private SocialImportResponse toResponse(SocialImport socialImport) {
        long analyzed = 0;
        long failed = 0;
        long pending = socialPostRepository.countBySocialImportIdAndAnalysisResultIdIsNull(socialImport.getId());
        for (Object[] row : socialPostRepository.countAnalysisStatuses(socialImport.getId())) {
            long count = ((Number) row[1]).longValue();
            switch ((AnalysisStatus) row[0]) {
                case OK -> analyzed += count;
                case FAILED -> failed += count;
                case PENDING -> pending += count;
            }
        }
        return SocialImportResponse.from(socialImport, analyzed, failed, pending);
    }

    // --- Helpers --------------------------------------------------------------------------

    private SocialImport requireImport(Long importId, Long userId) {
        // Scoped to the user, so another person's import reads as not found rather than forbidden.
        return socialImportRepository.findByIdAndUserId(importId, userId)
                .orElseThrow(() -> new ApiException("Import not found", HttpStatus.NOT_FOUND));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }

    private static String label(SocialProvider provider) {
        return switch (provider) {
            case X -> "X";
            case INSTAGRAM -> "Instagram";
            case FACEBOOK -> "Facebook";
        };
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
