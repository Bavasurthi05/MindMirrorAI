package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriggerEntryRepository extends JpaRepository<TriggerEntry, Long> {

    List<TriggerEntry> findByUserIdOrderByOccurredAtDesc(Long userId);

    /**
     * Triggers that count toward analytics: everything except the ones the user rejected.
     * Detected-but-unreviewed triggers are included so the dashboard is useful immediately.
     */
    List<TriggerEntry> findByUserIdAndConfirmationNotOrderByOccurredAtDesc(
            Long userId, TriggerConfirmation excluded);

    List<TriggerEntry> findByUserIdAndConfirmationOrderByOccurredAtDesc(
            Long userId, TriggerConfirmation confirmation);

    Optional<TriggerEntry> findByIdAndUserId(Long id, Long userId);
}
