package com.project.mentalhealth.application.service;

import com.project.mentalhealth.domain.model.SocialAccount;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.SocialAccountRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.social.dto.ConnectSocialAccountRequest;
import com.project.mentalhealth.interfaces.api.v1.social.dto.SocialAccountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialAccountServiceTest {

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SocialAccountService socialAccountService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
    }

    @Test
    void connectCreatesNewAccountForUser() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(socialAccountRepository.findByUserIdAndProviderAndExternalAccountId(42L, "instagram", "abc123"))
                .willReturn(Optional.empty());
        given(socialAccountRepository.save(any(SocialAccount.class))).willAnswer(invocation -> invocation.getArgument(0));

        SocialAccountResponse response = socialAccountService.connect("user@example.com", new ConnectSocialAccountRequest(
                "instagram",
                "abc123",
                "Ada's account",
                "https://example.com/avatar.png"
        ));

        assertThat(response.getProvider()).isEqualTo("instagram");
        assertThat(response.getDisplayName()).isEqualTo("Ada's account");
        assertThat(response.getStatus()).isEqualTo("CONNECTED");
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }

    @Test
    void listReturnsAccountsForTheAuthenticatedUser() {
        SocialAccount account = new SocialAccount();
        account.setId(7L);
        account.setUser(user);
        account.setProvider("x");
        account.setExternalAccountId("x-1");
        account.setDisplayName("Ada on X");
        account.setStatus("CONNECTED");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(socialAccountRepository.findByUserIdOrderByConnectedAtDesc(42L)).willReturn(List.of(account));

        List<SocialAccountResponse> response = socialAccountService.list("user@example.com");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getProvider()).isEqualTo("x");
        assertThat(response.get(0).getDisplayName()).isEqualTo("Ada on X");
    }
}
