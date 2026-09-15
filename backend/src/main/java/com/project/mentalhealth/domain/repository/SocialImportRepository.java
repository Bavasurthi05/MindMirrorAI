package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.SocialImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialImportRepository extends JpaRepository<SocialImport, Long> {

    List<SocialImport> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SocialImport> findByIdAndUserId(Long id, Long userId);
}
