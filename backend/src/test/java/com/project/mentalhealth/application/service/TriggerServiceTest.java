package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerAnalyticsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TriggerServiceTest {

    @Mock private TriggerEntryRepository triggerRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserPreferencesService preferencesService;

    private TriggerService service;

    @BeforeEach
    void setUp() {
        service = new TriggerService(triggerRepository, userRepository, preferencesService);
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(preferencesService.zoneFor(anyLong())).willReturn(ZoneOffset.UTC);
        given(triggerRepository.findByUserIdAndConfirmationNotOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of());
    }

    /** 2026-08-17 is a Monday. */
    private TriggerEntry trigger(String category, int intensity, String isoLocalDateTime) {
        TriggerEntry entry = new TriggerEntry();
        entry.setCategory(category);
        entry.setIntensity(intensity);
        entry.setOccurredAt(LocalDateTime.parse(isoLocalDateTime).toInstant(ZoneOffset.UTC));
        entry.setConfirmation(TriggerConfirmation.NOT_REQUIRED);
        return entry;
    }

    private TriggerAnalyticsResponse.HeatCell cell(TriggerAnalyticsResponse response, String day, String slot) {
        return response.getHeatmap().stream()
                .filter(item -> item.getDay().equals(day) && item.getSlot().equals(slot))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void slotsCoverEveryHourOfTheDay() {
        for (int hour = 0; hour < 24; hour++) {
            assertThat(TriggerService.slotForHour(hour)).isIn("Morning", "Midday", "Evening", "Night");
        }
    }

    @Test
    void slotBoundariesAreWhereTheyLook() {
        assertThat(TriggerService.slotForHour(5)).isEqualTo("Morning");
        assertThat(TriggerService.slotForHour(10)).isEqualTo("Morning");
        assertThat(TriggerService.slotForHour(11)).isEqualTo("Midday");
        assertThat(TriggerService.slotForHour(17)).isEqualTo("Evening");
        assertThat(TriggerService.slotForHour(22)).isEqualTo("Night");
        // Night wraps past midnight rather than starting a new bucket.
        assertThat(TriggerService.slotForHour(2)).isEqualTo("Night");
    }

    @Test
    void theGridIsAlwaysFullSoTheTableIsStable() {
        TriggerAnalyticsResponse response = service.analytics("user@example.com");

        assertThat(response.getHeatmap()).hasSize(28); // 7 days x 4 slots
        assertThat(response.getWeekdayIntensity()).hasSize(7);
    }

    @Test
    void emptyCellsCarryNullNotZero() {
        TriggerAnalyticsResponse response = service.analytics("user@example.com");

        // Zero would render as "calm"; null renders as "nothing logged".
        assertThat(response.getHeatmap()).allSatisfy(item -> {
            assertThat(item.getAverageIntensity()).isNull();
            assertThat(item.getCount()).isZero();
        });
    }

    @Test
    void triggersLandInTheRightDayAndSlot() {
        given(triggerRepository.findByUserIdAndConfirmationNotOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of(
                        trigger("Workload", 8, "2026-08-17T09:00:00"),   // Monday morning
                        trigger("Sleep", 4, "2026-08-19T23:30:00")));    // Wednesday night

        TriggerAnalyticsResponse response = service.analytics("user@example.com");

        assertThat(cell(response, "MONDAY", "Morning").getAverageIntensity()).isEqualTo(8.0);
        assertThat(cell(response, "MONDAY", "Morning").getCount()).isEqualTo(1);
        assertThat(cell(response, "WEDNESDAY", "Night").getAverageIntensity()).isEqualTo(4.0);
        assertThat(cell(response, "TUESDAY", "Morning").getAverageIntensity()).isNull();
    }

    @Test
    void repeatedTriggersInACellAverage() {
        given(triggerRepository.findByUserIdAndConfirmationNotOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of(
                        trigger("Workload", 8, "2026-08-17T09:00:00"),
                        trigger("Workload", 4, "2026-08-24T10:00:00")));

        assertThat(cell(service.analytics("user@example.com"), "MONDAY", "Morning").getAverageIntensity())
                .isEqualTo(6.0);
        assertThat(cell(service.analytics("user@example.com"), "MONDAY", "Morning").getCount())
                .isEqualTo(2);
    }

    @Test
    void bucketsFollowTheUsersTimezoneNotUtc() {
        // 22:00 UTC Monday is 08:00 Tuesday in Sydney — a different cell entirely.
        given(preferencesService.zoneFor(anyLong())).willReturn(ZoneId.of("Australia/Sydney"));
        given(triggerRepository.findByUserIdAndConfirmationNotOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of(trigger("Workload", 7, "2026-08-17T22:00:00")));

        TriggerAnalyticsResponse response = service.analytics("user@example.com");

        assertThat(cell(response, "TUESDAY", "Morning").getAverageIntensity()).isEqualTo(7.0);
        assertThat(cell(response, "MONDAY", "Night").getAverageIntensity()).isNull();
    }

    @Test
    void weekdayIntensityAveragesAcrossTheDay() {
        given(triggerRepository.findByUserIdAndConfirmationNotOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of(
                        trigger("Workload", 8, "2026-08-17T09:00:00"),
                        trigger("Sleep", 2, "2026-08-17T20:00:00")));

        TriggerAnalyticsResponse.WeekdayStat monday = service.analytics("user@example.com")
                .getWeekdayIntensity().stream()
                .filter(stat -> stat.getDay().equals("MONDAY"))
                .findFirst()
                .orElseThrow();

        assertThat(monday.getAverageIntensity()).isEqualTo(5.0);
        assertThat(monday.getCount()).isEqualTo(2);
    }

    @Test
    void daysWithNoTriggersHaveNoIntensity() {
        given(triggerRepository.findByUserIdAndConfirmationNotOrderByOccurredAtDesc(anyLong(), any()))
                .willReturn(List.of(trigger("Workload", 8, "2026-08-17T09:00:00")));

        assertThat(service.analytics("user@example.com").getWeekdayIntensity())
                .filteredOn(stat -> stat.getDay().equals("SUNDAY"))
                .allSatisfy(stat -> assertThat(stat.getAverageIntensity()).isNull());
    }
}
