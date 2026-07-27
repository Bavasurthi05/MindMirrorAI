package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.AssessmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentSubmissionRepository extends JpaRepository<AssessmentSubmission, Long> {
    List<AssessmentSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);
}
