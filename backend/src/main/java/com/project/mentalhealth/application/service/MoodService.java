package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.MoodUseCase;
import com.project.mentalhealth.domain.model.MoodEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.mood.dto.MoodEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.mood.dto.MoodEntryResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MoodService implements MoodUseCase {

    private final MoodEntryRepository moodRepository;
    private final UserRepository userRepository;

    public MoodService(MoodEntryRepository moodRepository, UserRepository userRepository) {
        this.moodRepository = moodRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public MoodEntryResponse log(String userEmail, MoodEntryRequest request) {
        User user = requireUser(userEmail);
        MoodEntry entry = new MoodEntry();
        entry.setUser(user);
        entry.setMoodScore(request.getMoodScore());
        entry.setMoodLabel(request.getMoodLabel());
        entry.setNote(request.getNote());
        entry.setRecordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now());
        return MoodEntryResponse.from(moodRepository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoodEntryResponse> recent(String userEmail, int days) {
        User user = requireUser(userEmail);
        int window = days < 1 ? 30 : days;
        Instant after = Instant.now().minus(window, ChronoUnit.DAYS);
        return moodRepository
                .findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(user.getId(), after)
                .stream()
                .map(MoodEntryResponse::from)
                .toList();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
