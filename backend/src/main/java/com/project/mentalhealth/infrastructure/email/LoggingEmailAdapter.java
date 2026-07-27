package com.project.mentalhealth.infrastructure.email;

import com.project.mentalhealth.application.ports.out.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development email adapter that logs messages instead of dispatching them.
 * Replace with an SMTP/provider-backed implementation for production.
 */
@Component
public class LoggingEmailAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailAdapter.class);

    @Override
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        log.info("[DEV EMAIL] Verification email to {}. Token: {}", toEmail, verificationToken);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("[DEV EMAIL] Password reset email to {}. Token: {}", toEmail, resetToken);
    }
}
