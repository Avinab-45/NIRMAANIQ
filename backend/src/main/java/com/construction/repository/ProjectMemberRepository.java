package com.construction.repository;

import com.construction.entity.ProjectMember;
import com.construction.entity.enums.MemberStatus;
import com.construction.entity.enums.ProjectMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    Optional<ProjectMember> findByProject_ProjectCodeAndUser_Id(String projectCode, UUID userId);
    Optional<ProjectMember> findByProject_IdAndUser_Id(UUID projectId, UUID userId);
    List<ProjectMember> findByProject_ProjectCode(String projectCode);
    List<ProjectMember> findByUser_Id(UUID userId);
    boolean existsByProject_ProjectCodeAndUser_IdAndStatus(String projectCode, UUID userId, MemberStatus status);
    boolean existsByProject_IdAndUser_IdAndStatus(UUID projectId, UUID userId, MemberStatus status);
    boolean existsByUser_IdAndRole(UUID userId, ProjectMemberRole role);
    boolean existsByUser_IdAndRoleAndStatus(UUID userId, ProjectMemberRole role, MemberStatus status);
    void deleteByProject_ProjectCodeAndUser_Id(String projectCode, UUID userId);
}
