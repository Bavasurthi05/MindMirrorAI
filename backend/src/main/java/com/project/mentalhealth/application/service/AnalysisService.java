package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.AnalysisUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AnalysisService implements AnalysisUseCase {

    private final MlAnalysisPort mlAnalysisPort;
    private final MoodEntryRepository moodRepository;
    private final UserRepository userRepository;

    public AnalysisService(MlAnalysisPort mlAnalysisPort,
                           MoodEntryRepository moodRepository,
                           UserRepository userRepository) {
        this.mlAnalysisPort = mlAnalysisPort;
        this.moodRepository = moodRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MlAnalysisPort.JournalAnalysis analyzeJournal(String text) {
        return mlAnalysisPort.analyzeJournal(text);
    }

    @Override
    public MlAnalysisPort.JournalAnalysis analyzeSocial(String text) {
        return mlAnalysisPort.analyzeSocial(text);
    }

    @Override
    public MlAnalysisPort.ModelMetrics modelMetrics() {
        return mlAnalysisPort.modelMetrics();
    }

    @Override
    @Transactional(readOnly = true)
    public MlAnalysisPort.MoodPrediction predictMood(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));

        List<MoodEntry> entries = moodRepository.findByUserIdOrderByRecordedAtDesc(user.getId());
        List<Integer> scores = new ArrayList<>(entries.stream().map(MoodEntry::getMoodScore).toList());
        Collections.reverse(scores); // chronological order for trend detection
        return mlAnalysisPort.predictMood(scores);
    }
}
