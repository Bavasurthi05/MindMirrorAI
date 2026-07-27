package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.TriggerUseCase;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerAnalyticsResponse;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerEntryResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TriggerService implements TriggerUseCase {

    private final TriggerEntryRepository triggerRepository;
    private final UserRepository userRepository;

    public TriggerService(TriggerEntryRepository triggerRepository, UserRepository userRepository) {
        this.triggerRepository = triggerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TriggerEntryResponse log(String userEmail, TriggerEntryRequest request) {
        User user = requireUser(userEmail);
        TriggerEntry entry = new TriggerEntry();
        entry.setUser(user);
        entry.setCategory(request.getCategory());
        entry.setIntensity(request.getIntensity());
        entry.setNote(request.getNote());
        entry.setOccurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now());
        return TriggerEntryResponse.from(triggerRepository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriggerEntryResponse> list(String userEmail) {
        User user = requireUser(userEmail);
        return triggerRepository.findByUserIdOrderByOccurredAtDesc(user.getId())
                .stream()
                .map(TriggerEntryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TriggerAnalyticsResponse analytics(String userEmail) {
        User user = requireUser(userEmail);
        List<TriggerEntry> entries = triggerRepository.findByUserIdOrderByOccurredAtDesc(user.getId());

        double overallAverage = entries.stream().mapToInt(TriggerEntry::getIntensity).average().orElse(0);

        Map<String, List<TriggerEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(TriggerEntry::getCategory));

        List<TriggerAnalyticsResponse.CategoryStat> categories = grouped.entrySet().stream()
                .map(e -> TriggerAnalyticsResponse.CategoryStat.builder()
                        .category(e.getKey())
                        .count(e.getValue().size())
                        .averageIntensity(round(e.getValue().stream().mapToInt(TriggerEntry::getIntensity).average().orElse(0)))
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .toList();

        return TriggerAnalyticsResponse.builder()
                .totalCount(entries.size())
                .averageIntensity(round(overallAverage))
                .categories(categories)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
