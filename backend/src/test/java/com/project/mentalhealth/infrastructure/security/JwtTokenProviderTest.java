package com.project.mentalhealth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-that-is-long-enough-for-hs256-signing");
        properties.setExpirationMs(3_600_000L);
        properties.setRefreshExpirationMs(604_800_000L);
        tokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void generatesAndValidatesToken() {
        String token = tokenProvider.generateToken("user@example.com", "ROLE_USER");

        assertTrue(tokenProvider.isValid(token));
        assertEquals("user@example.com", tokenProvider.getSubject(token));
        assertEquals("ROLE_USER", tokenProvider.getRole(token));
    }

    @Test
    void rejectsInvalidToken() {
        assertFalse(tokenProvider.isValid("not-a-real-token"));
    }
}
