package com.construction.repository;

import com.construction.entity.ProjectPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectPhaseRepository extends JpaRepository<ProjectPhase, UUID> {
    List<ProjectPhase> findByProject_ProjectCodeOrderBySequenceOrderAsc(String projectCode);
    Optional<ProjectPhase> findByIdAndProject_ProjectCode(UUID id, String projectCode);
}
