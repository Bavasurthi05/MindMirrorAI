package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.TriggerUseCase;
import com.project.mentalhealth.domain.model.TriggerConfirmation;
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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TriggerService implements TriggerUseCase {

    /** Time-of-day buckets, by local hour. Night wraps past midnight. */
    static final List<String> SLOTS = List.of("Morning", "Midday", "Evening", "Night");
    static final List<DayOfWeek> WEEK = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final TriggerEntryRepository triggerRepository;
    private final UserRepository userRepository;
    private final UserPreferencesService preferencesService;

    public TriggerService(TriggerEntryRepository triggerRepository,
                          UserRepository userRepository,
                          UserPreferencesService preferencesService) {
        this.triggerRepository = triggerRepository;
        this.userRepository = userRepository;
        this.preferencesService = preferencesService;
    }

    /** Which time-of-day bucket a local hour falls in. */
    static String slotForHour(int hour) {
        if (hour >= 5 && hour < 11) return "Morning";
        if (hour >= 11 && hour < 17) return "Midday";
        if (hour >= 17 && hour < 22) return "Evening";
        return "Night";
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
        return triggerRepository
                .findByUserIdAndConfirmationNotOrderByOccurredAtDesc(user.getId(), TriggerConfirmation.DISMISSED)
                .stream()
                .map(TriggerEntryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TriggerAnalyticsResponse analytics(String userEmail) {
        User user = requireUser(userEmail);
        List<TriggerEntry> entries = triggerRepository
                .findByUserIdAndConfirmationNotOrderByOccurredAtDesc(user.getId(), TriggerConfirmation.DISMISSED);

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

        ZoneId zone = preferencesService.zoneFor(user.getId());
        return TriggerAnalyticsResponse.builder()
                .totalCount(entries.size())
                .averageIntensity(round(overallAverage))
                .categories(categories)
                .heatmap(buildHeatmap(entries, zone))
                .weekdayIntensity(buildWeekdayIntensity(entries, zone))
                .build();
    }

    /**
     * Weekday × time-of-day intensity grid, always the full 7×4 so the UI can render a stable
     * table. Empty cells carry a null intensity rather than a zero, which would read as "calm"
     * instead of "nothing logged".
     */
    private List<TriggerAnalyticsResponse.HeatCell> buildHeatmap(List<TriggerEntry> entries, ZoneId zone) {
        Map<String, List<Integer>> grouped = new HashMap<>();
        for (TriggerEntry entry : entries) {
            ZonedDateTime local = entry.getOccurredAt().atZone(zone);
            String key = local.getDayOfWeek().name() + "|" + slotForHour(local.getHour());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(entry.getIntensity());
        }

        List<TriggerAnalyticsResponse.HeatCell> cells = new ArrayList<>();
        for (String slot : SLOTS) {
            for (DayOfWeek day : WEEK) {
                List<Integer> values = grouped.get(day.name() + "|" + slot);
                cells.add(TriggerAnalyticsResponse.HeatCell.builder()
                        .day(day.name())
                        .slot(slot)
                        .averageIntensity(values == null ? null
                                : round(values.stream().mapToInt(Integer::intValue).average().orElse(0)))
                        .count(values == null ? 0 : values.size())
                        .build());
            }
        }
        return cells;
    }

    private List<TriggerAnalyticsResponse.WeekdayStat> buildWeekdayIntensity(List<TriggerEntry> entries, ZoneId zone) {
        Map<DayOfWeek, List<Integer>> byDay = new HashMap<>();
        for (TriggerEntry entry : entries) {
            DayOfWeek day = entry.getOccurredAt().atZone(zone).getDayOfWeek();
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(entry.getIntensity());
        }
        return WEEK.stream()
                .map(day -> {
                    List<Integer> values = byDay.get(day);
                    return TriggerAnalyticsResponse.WeekdayStat.builder()
                            .day(day.name())
                            .averageIntensity(values == null ? null
                                    : round(values.stream().mapToInt(Integer::intValue).average().orElse(0)))
                            .count(values == null ? 0 : values.size())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriggerEntryResponse> pendingConfirmation(String userEmail) {
        User user = requireUser(userEmail);
        return triggerRepository
                .findByUserIdAndConfirmationOrderByOccurredAtDesc(user.getId(), TriggerConfirmation.PENDING)
                .stream()
                .map(TriggerEntryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public TriggerEntryResponse confirm(String userEmail, Long id, Integer intensity) {
        TriggerEntry entry = requireEntry(userEmail, id);
        if (intensity != null) {
            if (intensity < 1 || intensity > 10) {
                throw new ApiException("Intensity must be between 1 and 10", HttpStatus.BAD_REQUEST);
            }
            entry.setIntensity(intensity);
        }
        entry.setConfirmation(TriggerConfirmation.CONFIRMED);
        return TriggerEntryResponse.from(triggerRepository.save(entry));
    }

    @Override
    @Transactional
    public TriggerEntryResponse dismiss(String userEmail, Long id) {
        TriggerEntry entry = requireEntry(userEmail, id);
        // Kept rather than deleted: a wrong detection is useful signal for tuning the lexicon.
        entry.setConfirmation(TriggerConfirmation.DISMISSED);
        return TriggerEntryResponse.from(triggerRepository.save(entry));
    }

    private TriggerEntry requireEntry(String userEmail, Long id) {
        User user = requireUser(userEmail);
        return triggerRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException("Trigger not found", HttpStatus.NOT_FOUND));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
