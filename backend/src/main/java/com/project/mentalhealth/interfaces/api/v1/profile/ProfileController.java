package com.project.mentalhealth.interfaces.api.v1.profile;

import com.project.mentalhealth.application.ports.in.ProfileUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.ChangePasswordRequest;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.ProfileResponse;
import com.project.mentalhealth.interfaces.api.v1.profile.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PatchMapping("/profile")
    public ApiResponse<ProfileResponse> updateProfile(Authentication authentication,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(profileUseCase.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(Authentication authentication,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        profileUseCase.changePassword(authentication.getName(), request);
        return ApiResponse.success(null, "Password updated successfully");
    }
}
