package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.FeedbackAgreement;
import com.project.mentalhealth.domain.model.PredictionFeedback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionFeedbackRepository extends JpaRepository<PredictionFeedback, Long> {

    Optional<PredictionFeedback> findByAnalysisResultId(Long analysisResultId);

    List<PredictionFeedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<PredictionFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByAgreement(FeedbackAgreement agreement);

    /** Corrected-label distribution, so an admin can see whether classes are balanced enough to train on. */
    @Query("SELECT f.correctedLabel, COUNT(f) FROM PredictionFeedback f "
            + "WHERE f.correctedLabel IS NOT NULL GROUP BY f.correctedLabel")
    List<Object[]> countByCorrectedLabel();
}
