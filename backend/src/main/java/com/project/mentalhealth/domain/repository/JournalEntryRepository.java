package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Page<JournalEntry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);
    java.util.List<JournalEntry> findByUserIdOrderByCreatedAtDesc(Long userId);
}
