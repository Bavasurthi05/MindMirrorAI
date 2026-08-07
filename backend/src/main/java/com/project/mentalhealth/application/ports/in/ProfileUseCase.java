package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.profile.dto.ChangePasswordRequest;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.ProfileResponse;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.UpdateProfileRequest;

public interface ProfileUseCase {
    ProfileResponse profile(String userEmail);

    ProfileResponse updateProfile(String userEmail, UpdateProfileRequest request);

    void changePassword(String userEmail, ChangePasswordRequest request);
}
