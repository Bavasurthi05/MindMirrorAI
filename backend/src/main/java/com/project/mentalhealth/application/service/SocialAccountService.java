package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.SocialAccountUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.AssessmentSubmission;
import com.project.mentalhealth.domain.model.SocialAccount;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.SocialAccountRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.social.dto.ConnectSocialAccountRequest;
import com.project.mentalhealth.interfaces.api.v1.social.dto.SocialAccountResponse;
import com.project.mentalhealth.interfaces.api.v1.social.dto.SocialContentImportRequest;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class SocialAccountService implements SocialAccountUseCase {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final AssessmentSubmissionRepository assessmentRepository;
    private final MlAnalysisPort mlAnalysisPort;

    public SocialAccountService(SocialAccountRepository socialAccountRepository,
                                UserRepository userRepository,
                                AssessmentSubmissionRepository assessmentRepository,
                                MlAnalysisPort mlAnalysisPort) {
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
        this.assessmentRepository = assessmentRepository;
        this.mlAnalysisPort = mlAnalysisPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SocialAccountResponse> list(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return socialAccountRepository.findByUserIdOrderByConnectedAtDesc(user.getId()).stream()
                .map(SocialAccountResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SocialAccountResponse connect(String userEmail, ConnectSocialAccountRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));

        SocialAccount existing = socialAccountRepository
                .findByUserIdAndProviderAndExternalAccountId(user.getId(), request.getProvider(), request.getExternalAccountId())
                .orElse(null);

        if (existing != null) {
            existing.setDisplayName(request.getDisplayName());
            existing.setProfileImageUrl(request.getProfileImageUrl());
            existing.setStatus("CONNECTED");
            existing.setConnectedAt(existing.getConnectedAt() == null ? Instant.now() : existing.getConnectedAt());
            existing.setLastSyncedAt(Instant.now());
            existing.setOauthScope(request.getScope());
            return SocialAccountResponse.from(socialAccountRepository.save(existing));
        }

        SocialAccount account = new SocialAccount();
        account.setUser(user);
        account.setProvider(request.getProvider().toLowerCase());
        account.setExternalAccountId(request.getExternalAccountId());
        account.setDisplayName(request.getDisplayName());
        account.setProfileImageUrl(request.getProfileImageUrl());
        account.setStatus("CONNECTED");
        account.setConnectedAt(Instant.now());
        account.setLastSyncedAt(Instant.now());
        account.setOauthScope(request.getScope());
        return SocialAccountResponse.from(socialAccountRepository.save(account));
    }

    @Override
    @Transactional
    public void disconnect(String userEmail, Long accountId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));

        SocialAccount account = socialAccountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException("Social account not found", HttpStatus.NOT_FOUND));

        if (!Objects.equals(account.getUser().getId(), user.getId())) {
            throw new ApiException("You do not have access to this account", HttpStatus.FORBIDDEN);
        }

        account.setStatus("DISCONNECTED");
        socialAccountRepository.save(account);
    }

    @Transactional
    public MlAnalysisPort.JournalAnalysis importSocialContent(String userEmail, SocialContentImportRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));

        MlAnalysisPort.JournalAnalysis analysis = mlAnalysisPort.analyzeSocial(request.getContent());

        AssessmentSubmission submission = new AssessmentSubmission();
        submission.setUser(user);
        submission.setQuestionnaireKey("social-import:" + request.getProvider().toLowerCase());
        submission.setTotalScore((int) Math.round(analysis.sentimentScore() * 100));
        submission.setMaxScore(100);
        submission.setSeverity(analysis.prediction());
        submission.setAnswers(request.getContent());
        submission.setSubmittedAt(Instant.now());
        assessmentRepository.save(submission);

        return analysis;
    }
}
