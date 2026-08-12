package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.ModelTrainingRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelTrainingRunRepository extends JpaRepository<ModelTrainingRun, Long> {
    List<ModelTrainingRun> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Optional<ModelTrainingRun> findFirstByJobIdOrderByIdDesc(String jobId);
    Optional<ModelTrainingRun> findFirstByVersionOrderByIdDesc(String version);
}
