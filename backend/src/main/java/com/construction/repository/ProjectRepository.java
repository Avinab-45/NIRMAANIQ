package com.construction.repository;

import com.construction.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByProjectCode(String projectCode);
    boolean existsByProjectCode(String projectCode);

    Page<Project> findByCreatedBy_Id(UUID createdById, Pageable pageable);

    @Query("SELECT DISTINCT pm.project FROM ProjectMember pm WHERE pm.user.id = :userId AND pm.status = 'ACTIVE'")
    Page<Project> findAssignedProjectsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
