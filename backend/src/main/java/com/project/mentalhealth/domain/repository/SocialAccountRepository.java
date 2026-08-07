package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    List<SocialAccount> findByUserIdOrderByConnectedAtDesc(Long userId);

    Optional<SocialAccount> findByUserIdAndProviderAndExternalAccountId(Long userId, String provider, String externalAccountId);
}
