package com.project.mentalhealth.interfaces.api.v1.analysis.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mentalhealth.domain.model.AnalysisResult;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A stored analysis, with its JSON columns expanded back into real structures. */
@Getter
@Builder
public class AnalysisResultResponse {

    private final Long id;
    private final String sourceType;
    private final Long sourceId;
    private final String status;
    private final String errorMessage;
    private final String sentiment;
    private final Double sentimentScore;
    private final String dominantEmotion;
    private final Map<String, Double> emotionScores;
    private final String prediction;
    private final Double predictionConfidence;
    private final Map<String, Double> predictionProbabilities;
    private final List<Reason> reasons;
    private final List<TokenContribution> explanation;
    private final List<Trigger> triggers;
    private final String modelBackend;
    private final String modelVersion;
    private final Instant analyzedAt;

    // Records with explicit @JsonProperty: these are read back out of the stored JSON
    // columns, so they must deserialize regardless of the -parameters compiler flag.
    public record Reason(@JsonProperty("feature") String feature,
                         @JsonProperty("weight") double weight,
                         @JsonProperty("percentage") double percentage) {}

    public record TokenContribution(@JsonProperty("token") String token,
                                    @JsonProperty("weight") double weight) {}

    public record Trigger(@JsonProperty("category") String category,
                          @JsonProperty("matchedTerms") List<String> matchedTerms,
                          @JsonProperty("intensity") int intensity) {}

    public static AnalysisResultResponse from(AnalysisResult entity, ObjectMapper mapper) {
        return AnalysisResultResponse.builder()
                .id(entity.getId())
                .sourceType(entity.getSourceType().name())
                .sourceId(entity.getSourceId())
                .status(entity.getStatus().name())
                .errorMessage(entity.getErrorMessage())
                .sentiment(entity.getSentiment())
                .sentimentScore(entity.getSentimentScore())
                .dominantEmotion(entity.getDominantEmotion())
                .emotionScores(readMap(entity.getEmotionScores(), mapper))
                .prediction(entity.getPrediction())
                .predictionConfidence(entity.getPredictionConfidence())
                .predictionProbabilities(readMap(entity.getPredictionProbabilities(), mapper))
                .reasons(readList(entity.getReasons(), mapper, new TypeReference<List<Reason>>() {}))
                .explanation(readList(entity.getExplanation(), mapper, new TypeReference<List<TokenContribution>>() {}))
                .triggers(readList(entity.getDetectedTriggers(), mapper, new TypeReference<List<Trigger>>() {}))
                .modelBackend(entity.getModelBackend())
                .modelVersion(entity.getModelVersion())
                .analyzedAt(entity.getAnalyzedAt())
                .build();
    }

    private static Map<String, Double> readMap(String json, ObjectMapper mapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Double>>() {});
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private static <T> List<T> readList(String json, ObjectMapper mapper, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
