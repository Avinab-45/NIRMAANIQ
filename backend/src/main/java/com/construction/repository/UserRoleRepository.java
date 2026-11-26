package com.construction.repository;

import com.construction.entity.UserRole;
import com.construction.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUser_Id(UUID userId);
    Optional<UserRole> findByUser_IdAndRole_Name(UUID userId, RoleType roleName);
    boolean existsByUser_IdAndRole_Name(UUID userId, RoleType roleName);
    void deleteByUser_Id(UUID userId);
}
