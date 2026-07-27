package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.MoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MoodEntryRepository extends JpaRepository<MoodEntry, Long> {
    List<MoodEntry> findByUserIdAndRecordedAtAfterOrderByRecordedAtDesc(Long userId, Instant after);
    List<MoodEntry> findByUserIdOrderByRecordedAtDesc(Long userId);
}
