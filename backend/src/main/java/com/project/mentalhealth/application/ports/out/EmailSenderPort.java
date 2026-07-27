package com.project.mentalhealth.application.ports.out;

public interface EmailSenderPort {
    void sendVerificationEmail(String toEmail, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String resetToken);
}
