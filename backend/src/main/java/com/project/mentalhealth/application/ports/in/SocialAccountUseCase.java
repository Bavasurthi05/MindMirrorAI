package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.social.dto.ConnectSocialAccountRequest;
import com.project.mentalhealth.interfaces.api.v1.social.dto.SocialAccountResponse;

import java.util.List;

public interface SocialAccountUseCase {
    List<SocialAccountResponse> list(String userEmail);

    SocialAccountResponse connect(String userEmail, ConnectSocialAccountRequest request);

    void disconnect(String userEmail, Long accountId);
}
