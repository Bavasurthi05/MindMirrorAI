package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.RecoveryAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryActionRepository extends JpaRepository<RecoveryAction, Long> {
    List<RecoveryAction> findByUserIdOrderByIdAsc(Long userId);
    Optional<RecoveryAction> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
}
