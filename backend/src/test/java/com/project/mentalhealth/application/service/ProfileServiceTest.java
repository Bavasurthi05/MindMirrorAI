package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.out.PasswordEncoderPort;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.domain.repository.WellnessGoalRepository;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.ChangePasswordRequest;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.UpdateProfileRequest;
import com.project.mentalhealth.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JournalEntryRepository journalRepository;

    @Mock
    private MoodEntryRepository moodRepository;

    @Mock
    private WellnessGoalRepository goalRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateProfilePersistsNameChanges() {
        User user = new User();
        user.setId(1L);
        user.setEmail("jane@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(journalRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(moodRepository.findByUserIdOrderByRecordedAtDesc(1L)).thenReturn(List.of());
        when(goalRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of());

        var response = profileService.updateProfile("jane@example.com", new UpdateProfileRequest("Jane", "Smith"));

        assertEquals("Jane Smith", response.getFullName());
        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        verify(userRepository).save(user);
        assertEquals("Smith", user.getLastName());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = new User();
        user.setPasswordHash("encoded");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-current", "encoded")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> profileService.changePassword("jane@example.com", new ChangePasswordRequest("wrong-current", "new-password")));

        assertEquals("Current password is incorrect", exception.getMessage());
    }
}
