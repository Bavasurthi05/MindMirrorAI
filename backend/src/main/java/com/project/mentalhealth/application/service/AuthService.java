package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.AuthUseCase;
import com.project.mentalhealth.application.ports.out.EmailSenderPort;
import com.project.mentalhealth.application.ports.out.PasswordEncoderPort;
import com.project.mentalhealth.domain.model.EmailVerificationToken;
import com.project.mentalhealth.domain.model.PasswordResetToken;
import com.project.mentalhealth.domain.model.RefreshToken;
import com.project.mentalhealth.domain.model.Role;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.EmailVerificationTokenRepository;
import com.project.mentalhealth.domain.repository.PasswordResetTokenRepository;
import com.project.mentalhealth.domain.repository.RefreshTokenRepository;
import com.project.mentalhealth.domain.repository.RoleRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.infrastructure.security.JwtProperties;
import com.project.mentalhealth.infrastructure.security.JwtTokenProvider;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.AuthResponse;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.ForgotPasswordRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.LoginRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.RefreshTokenRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.RegisterRequest;
import com.project.mentalhealth.interfaces.api.v1.auth.dto.ResetPasswordRequest;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final long VERIFICATION_TTL_MS = 86_400_000L;
    private static final long RESET_TTL_MS = 3_600_000L;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final EmailSenderPort emailSender;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoderPort passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       JwtProperties jwtProperties,
                       EmailSenderPort emailSender) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.emailSender = emailSender;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("An account with this email already exists", HttpStatus.CONFLICT);
        }

        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ApiException("Default role not configured", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setRole(role);

        User saved = userRepository.save(user);
        issueVerificationToken(saved);
        return buildAuthResponse(saved);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isEnabled()) {
            throw new ApiException("This account is disabled", HttpStatus.FORBIDDEN);
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("Refresh token expired. Please sign in again.", HttpStatus.UNAUTHORIZED);
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return buildAuthResponse(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verification = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ApiException("Invalid verification token", HttpStatus.BAD_REQUEST));

        if (verification.isUsed() || verification.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("Verification link is invalid or expired", HttpStatus.BAD_REQUEST);
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setUsed(true);
        emailVerificationTokenRepository.save(verification);
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        // Always respond success to avoid leaking which emails are registered.
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(Instant.now().plusMillis(RESET_TTL_MS));
            resetToken.setUsed(false);
            passwordResetTokenRepository.save(resetToken);
            emailSender.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ApiException("Invalid reset token", HttpStatus.BAD_REQUEST));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("Reset link is invalid or expired", HttpStatus.BAD_REQUEST);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Invalidate existing sessions after a password change.
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    private void issueVerificationToken(User user) {
        EmailVerificationToken verification = new EmailVerificationToken();
        verification.setUser(user);
        verification.setToken(UUID.randomUUID().toString());
        verification.setExpiresAt(Instant.now().plusMillis(VERIFICATION_TTL_MS));
        verification.setUsed(false);
        emailVerificationTokenRepository.save(verification);
        emailSender.sendVerificationEmail(user.getEmail(), verification.getToken());
    }

    private AuthResponse buildAuthResponse(User user) {
        String roleName = user.getRole().getName();
        String accessToken = tokenProvider.generateToken(user.getEmail(), roleName);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(roleName)
                .emailVerified(user.isEmailVerified())
                .build();
    }
}
