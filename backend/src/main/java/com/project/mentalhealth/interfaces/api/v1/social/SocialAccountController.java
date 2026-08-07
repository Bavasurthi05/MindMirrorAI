package com.project.mentalhealth.interfaces.api.v1.social;

import com.project.mentalhealth.application.ports.in.SocialAccountUseCase;
import com.project.mentalhealth.application.service.SocialAccountService;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.social.dto.ConnectSocialAccountRequest;
import com.project.mentalhealth.interfaces.api.v1.social.dto.SocialAccountResponse;
import com.project.mentalhealth.interfaces.api.v1.social.dto.SocialContentImportRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/social-accounts")
public class SocialAccountController {

    private final SocialAccountUseCase socialAccountUseCase;
    private final SocialAccountService socialAccountService;

    public SocialAccountController(SocialAccountUseCase socialAccountUseCase, SocialAccountService socialAccountService) {
        this.socialAccountUseCase = socialAccountUseCase;
        this.socialAccountService = socialAccountService;
    }

    @GetMapping
    public ApiResponse<List<SocialAccountResponse>> list(Authentication authentication) {
        return ApiResponse.success(socialAccountUseCase.list(authentication.getName()));
    }

    @PostMapping
    public ApiResponse<SocialAccountResponse> connect(Authentication authentication,
                                                     @Valid @RequestBody ConnectSocialAccountRequest request) {
        return ApiResponse.success(socialAccountUseCase.connect(authentication.getName(), request));
    }

    @PostMapping("/import")
    public ApiResponse<Object> importContent(Authentication authentication,
                                             @Valid @RequestBody SocialContentImportRequest request) {
        return ApiResponse.success(socialAccountService.importSocialContent(authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> disconnect(Authentication authentication, @PathVariable Long id) {
        socialAccountUseCase.disconnect(authentication.getName(), id);
        return ApiResponse.success(null);
    }
}
