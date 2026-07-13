package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.auth.dto.AuthResponse;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.LoginRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.RegisterRequest;

public interface AuthUseCase {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
}
