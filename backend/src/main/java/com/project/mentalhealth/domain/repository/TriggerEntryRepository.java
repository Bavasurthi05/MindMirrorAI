package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.TriggerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriggerEntryRepository extends JpaRepository<TriggerEntry, Long> {
    List<TriggerEntry> findByUserIdOrderByOccurredAtDesc(Long userId);
}
