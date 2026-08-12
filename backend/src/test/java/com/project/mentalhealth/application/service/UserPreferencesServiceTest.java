package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.model.UserPreferences;
import com.project.mentalhealth.domain.repository.UserPreferencesRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.UpdatePreferencesRequest;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.UserPreferencesResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserPreferencesServiceTest {

    @Mock private UserPreferencesRepository preferencesRepository;
    @Mock private UserRepository userRepository;

    private UserPreferencesService service;

    @BeforeEach
    void setUp() {
        service = new UserPreferencesService(preferencesRepository, userRepository);
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(preferencesRepository.findByUserId(anyLong())).willReturn(Optional.empty());
        given(preferencesRepository.save(any(UserPreferences.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void firstAccessCreatesSafeDefaults() {
        UserPreferencesResponse response = service.get("user@example.com");

        assertThat(response.getTimezone()).isEqualTo("UTC");
        assertThat(response.isReminderEnabled()).isFalse();
        // Training consent must never default to on.
        assertThat(response.isTrainingConsent()).isFalse();
        assertThat(response.isOnboardingCompleted()).isFalse();
    }

    @Test
    void grantingConsentStampsTheMoment() {
        UpdatePreferencesRequest request = new UpdatePreferencesRequest();
        request.setTrainingConsent(true);

        UserPreferencesResponse response = service.update("user@example.com", request);

        assertThat(response.isTrainingConsent()).isTrue();
        assertThat(response.getTrainingConsentAt()).isNotNull();
    }

    @Test
    void revokingConsentIsAlsoStamped() {
        UserPreferences existing = new UserPreferences();
        existing.setTrainingConsent(true);
        given(preferencesRepository.findByUserId(anyLong())).willReturn(Optional.of(existing));

        UpdatePreferencesRequest request = new UpdatePreferencesRequest();
        request.setTrainingConsent(false);

        UserPreferencesResponse response = service.update("user@example.com", request);

        assertThat(response.isTrainingConsent()).isFalse();
        assertThat(response.getTrainingConsentAt()).isNotNull();
    }

    @Test
    void unsetFieldsAreLeftUnchanged() {
        UserPreferences existing = new UserPreferences();
        existing.setTimezone("Australia/Sydney");
        existing.setReminderEnabled(true);
        given(preferencesRepository.findByUserId(anyLong())).willReturn(Optional.of(existing));

        UserPreferencesResponse response = service.update("user@example.com", new UpdatePreferencesRequest());

        assertThat(response.getTimezone()).isEqualTo("Australia/Sydney");
        assertThat(response.isReminderEnabled()).isTrue();
    }

    @Test
    void anUnknownTimezoneIsRejected() {
        UpdatePreferencesRequest request = new UpdatePreferencesRequest();
        request.setTimezone("Mars/Olympus_Mons");

        assertThatThrownBy(() -> service.update("user@example.com", request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown timezone");
    }

    @Test
    void focusAreasRoundTripThroughStorage() {
        UpdatePreferencesRequest request = new UpdatePreferencesRequest();
        request.setFocusAreas(List.of("sleep", "work"));

        assertThat(service.update("user@example.com", request).getFocusAreas())
                .containsExactly("sleep", "work");
    }

    @Test
    void zoneForFallsBackToUtcWhenNoPreferencesExist() {
        assertThat(service.zoneFor(1L)).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void aCorruptStoredTimezoneDoesNotBreakReads() {
        UserPreferences broken = new UserPreferences();
        broken.setTimezone("not-a-zone");
        given(preferencesRepository.findByUserId(anyLong())).willReturn(Optional.of(broken));

        assertThat(service.zoneFor(1L)).isEqualTo(ZoneId.of("UTC"));
    }
}
