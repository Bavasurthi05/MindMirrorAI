package com.project.mentalhealth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.mentalhealth.application.ports.in.AnalysisUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AnalysisResultRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.analysis.dto.AnalysisResultResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AnalysisService implements AnalysisUseCase {

    /** Mood prediction only looks at the recent window; older entries say little about now. */
    private static final int MOOD_PREDICTION_WINDOW = 30;
    private static final int MAX_RESULT_LIMIT = 50;

    private final MlAnalysisPort mlAnalysisPort;
    private final MoodEntryRepository moodRepository;
    private final UserRepository userRepository;
    private final AnalysisResultRepository analysisRepository;
    private final AnalysisOrchestrator analysisOrchestrator;
    private final ObjectMapper objectMapper;

    public AnalysisService(MlAnalysisPort mlAnalysisPort,
                           MoodEntryRepository moodRepository,
                           UserRepository userRepository,
                           AnalysisResultRepository analysisRepository,
                           AnalysisOrchestrator analysisOrchestrator,
                           ObjectMapper objectMapper) {
        this.mlAnalysisPort = mlAnalysisPort;
        this.moodRepository = moodRepository;
        this.userRepository = userRepository;
        this.analysisRepository = analysisRepository;
        this.analysisOrchestrator = analysisOrchestrator;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AnalysisResultResponse analyzeJournal(String userEmail, String text) {
        return analyzeNow(userEmail, AnalysisSourceType.ADHOC, text);
    }

    @Override
    @Transactional
    public AnalysisResultResponse analyzeSocial(String userEmail, String text) {
        return analyzeNow(userEmail, AnalysisSourceType.SOCIAL, text);
    }

    private AnalysisResultResponse analyzeNow(String userEmail, AnalysisSourceType sourceType, String text) {
        User user = requireUser(userEmail);
        AnalysisOrchestrator.CompletedAnalysis completed =
                analysisOrchestrator.submitAndWait(user, sourceType, null, text);
        if (!completed.isOk()) {
            throw new ApiException("ML service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return AnalysisResultResponse.from(completed.result(), objectMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisResultResponse> recentResults(String userEmail, int limit) {
        User user = requireUser(userEmail);
        int capped = Math.max(1, Math.min(limit, MAX_RESULT_LIMIT));
        return analysisRepository
                .findByUserIdAndStatusOrderByAnalyzedAtDesc(user.getId(), AnalysisStatus.OK, PageRequest.of(0, capped))
                .stream()
                .map(entity -> AnalysisResultResponse.from(entity, objectMapper))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisResultResponse result(String userEmail, Long id) {
        User user = requireUser(userEmail);
        AnalysisResult entity = analysisRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException("Analysis not found", HttpStatus.NOT_FOUND));
        return AnalysisResultResponse.from(entity, objectMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisResultResponse resultForJournalEntry(String userEmail, Long journalEntryId) {
        User user = requireUser(userEmail);
        return analysisRepository
                .findFirstByUserIdAndSourceTypeAndSourceIdOrderByIdDesc(
                        user.getId(), AnalysisSourceType.JOURNAL, journalEntryId)
                .map(entity -> AnalysisResultResponse.from(entity, objectMapper))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MlAnalysisPort.MoodPrediction predictMood(String userEmail) {
        User user = requireUser(userEmail);
        List<MoodEntry> entries = moodRepository.findByUserIdOrderByRecordedAtDesc(user.getId());
        List<Integer> scores = new ArrayList<>(entries.stream()
                .limit(MOOD_PREDICTION_WINDOW)
                .map(MoodEntry::getMoodScore)
                .toList());
        Collections.reverse(scores); // chronological order for trend detection
        return mlAnalysisPort.predictMood(scores);
    }

    @Override
    public MlAnalysisPort.ModelMetrics modelMetrics() {
        return mlAnalysisPort.modelMetrics();
    }

    @Override
    public MlAnalysisPort.MlHealth mlHealth() {
        return mlAnalysisPort.health();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
