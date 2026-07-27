package com.project.mentalhealth.interfaces.api.v1.auth;

import com.project.mentalhealth.application.ports.in.AuthUseCase;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.AuthResponse;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.ForgotPasswordRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.LoginRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.RefreshTokenRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.RegisterRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.ResetPasswordRequest;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("Auth service is up");
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authUseCase.register(request), "Registration successful");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authUseCase.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authUseCase.refresh(request), "Token refreshed");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authUseCase.logout(request);
        return ApiResponse.success(null, "Logged out");
    }

    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam("token") String token) {
        authUseCase.verifyEmail(token);
        return ApiResponse.success(null, "Email verified");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authUseCase.requestPasswordReset(request);
        return ApiResponse.success(null, "If an account exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authUseCase.resetPassword(request);
        return ApiResponse.success(null, "Password updated");
    }
}
