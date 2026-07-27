package com.project.mentalhealth.interfaces.api.v1.profile;

import com.project.mentalhealth.application.ports.in.ProfileUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.ProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/me")
public class ProfileController {

    private final ProfileUseCase profileUseCase;

    public ProfileController(ProfileUseCase profileUseCase) {
        this.profileUseCase = profileUseCase;
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(Authentication authentication) {
        return ApiResponse.success(profileUseCase.profile(authentication.getName()));
    }
}
