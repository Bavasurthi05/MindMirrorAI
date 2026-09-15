package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.AnalysisResult;
import com.project.mentalhealth.domain.model.AnalysisSourceType;
import com.project.mentalhealth.domain.model.AnalysisStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    List<AnalysisResult> findByUserIdAndStatusOrderByAnalyzedAtDesc(Long userId, AnalysisStatus status, Pageable pageable);

    List<AnalysisResult> findByUserIdAndSourceTypeAndStatusOrderByAnalyzedAtDesc(
            Long userId, AnalysisSourceType sourceType, AnalysisStatus status, Pageable pageable);

    /**
     * Completed analyses since a point in time, excluding one source.
     *
     * <p>Callers pass {@code SOCIAL}: an imported archive is analyzed all at once, so its rows
     * would otherwise all land "today" and swamp the user's real day-to-day signal.
     */
    List<AnalysisResult> findByUserIdAndStatusAndSourceTypeNotAndAnalyzedAtAfterOrderByAnalyzedAtDesc(
            Long userId, AnalysisStatus status, AnalysisSourceType excluded, Instant after);

    List<AnalysisResult> findByUserIdAndStatusAndSourceTypeNotOrderByAnalyzedAtDesc(
            Long userId, AnalysisStatus status, AnalysisSourceType excluded, Pageable pageable);

    Optional<AnalysisResult> findByIdAndUserId(Long id, Long userId);

    Optional<AnalysisResult> findFirstByUserIdAndSourceTypeAndSourceIdOrderByIdDesc(
            Long userId, AnalysisSourceType sourceType, Long sourceId);

    List<AnalysisResult> findByStatusAndAttemptCountLessThanOrderByIdAsc(
            AnalysisStatus status, int maxAttempts, Pageable pageable);

    long countByUserIdAndStatusAndSourceTypeNot(Long userId, AnalysisStatus status, AnalysisSourceType excluded);
}
