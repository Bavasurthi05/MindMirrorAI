package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.profile.dto.ProfileResponse;

public interface ProfileUseCase {
    ProfileResponse profile(String userEmail);
}
