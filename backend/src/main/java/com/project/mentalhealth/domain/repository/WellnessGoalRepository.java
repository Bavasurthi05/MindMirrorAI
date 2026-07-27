package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.WellnessGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WellnessGoalRepository extends JpaRepository<WellnessGoal, Long> {
    List<WellnessGoal> findByUserIdOrderByIdAsc(Long userId);
    Optional<WellnessGoal> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
}
