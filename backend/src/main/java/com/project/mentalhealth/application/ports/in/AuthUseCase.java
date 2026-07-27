package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.auth.dto.AuthResponse;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.ForgotPasswordRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.LoginRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.RefreshTokenRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.RegisterRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.ResetPasswordRequest;

public interface AuthUseCase {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
    void verifyEmail(String token);
    void requestPasswordReset(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
