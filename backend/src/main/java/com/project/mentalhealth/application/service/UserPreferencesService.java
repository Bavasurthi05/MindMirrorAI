package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.model.UserPreferences;
import com.project.mentalhealth.domain.repository.UserPreferencesRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.UpdatePreferencesRequest;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.UserPreferencesResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Reads and updates per-user preferences, creating defaults on first access so callers never
 * have to deal with an absent row.
 */
@Service
public class UserPreferencesService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferencesService.class);

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    public UserPreferencesService(UserPreferencesRepository preferencesRepository,
                                  UserRepository userRepository) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserPreferencesResponse get(String userEmail) {
        return UserPreferencesResponse.from(requirePreferences(requireUser(userEmail)));
    }

    @Transactional
    public UserPreferencesResponse update(String userEmail, UpdatePreferencesRequest request) {
        UserPreferences preferences = requirePreferences(requireUser(userEmail));

        if (request.getTimezone() != null) {
            preferences.setTimezone(validateTimezone(request.getTimezone()));
        }
        if (request.getReminderEnabled() != null) {
            preferences.setReminderEnabled(request.getReminderEnabled());
        }
        if (request.getReminderTime() != null) {
            preferences.setReminderTime(request.getReminderTime());
        }
        if (request.getFocusAreas() != null) {
            preferences.setFocusAreas(String.join(",", request.getFocusAreas()));
        }
        if (request.getTrainingConsent() != null
                && request.getTrainingConsent() != preferences.isTrainingConsent()) {
            // Timestamp the moment consent changes, in either direction.
            preferences.setTrainingConsent(request.getTrainingConsent());
            preferences.setTrainingConsentAt(Instant.now());
        }
        if (request.getOnboardingStep() != null) {
            preferences.setOnboardingStep(request.getOnboardingStep());
        }
        if (request.getOnboardingCompleted() != null) {
            preferences.setOnboardingCompleted(request.getOnboardingCompleted());
        }

        return UserPreferencesResponse.from(preferencesRepository.save(preferences));
    }

    /** The user's zone, falling back to UTC rather than failing a request over a bad value. */
    @Transactional(readOnly = true)
    public ZoneId zoneFor(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getTimezone)
                .map(this::toZoneIdOrUtc)
                .orElse(ZoneId.of(UserPreferences.DEFAULT_TIMEZONE));
    }

    /**
     * Preferences for a read-only caller: returns transient defaults rather than inserting.
     *
     * <p>Read paths (dashboard, analytics, reports) run in read-only transactions, so a lazy
     * insert here would fail the whole request for any user who has never opened Settings.
     */
    @Transactional(readOnly = true)
    public UserPreferences preferencesOrDefaults(User user) {
        return preferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPreferences defaults = new UserPreferences();
                    defaults.setUser(user);
                    return defaults;
                });
    }

    @Transactional
    public UserPreferences requirePreferences(User user) {
        return preferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPreferences created = new UserPreferences();
                    created.setUser(user);
                    return preferencesRepository.save(created);
                });
    }

    private String validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (DateTimeException ex) {
            throw new ApiException("Unknown timezone: " + timezone, HttpStatus.BAD_REQUEST);
        }
    }

    private ZoneId toZoneIdOrUtc(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            log.warn("Stored timezone '{}' is not valid; falling back to UTC", timezone);
            return ZoneId.of(UserPreferences.DEFAULT_TIMEZONE);
        }
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
