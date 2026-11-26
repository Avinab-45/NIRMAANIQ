package com.construction.repository;

import com.construction.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    Page<Activity> findByProject_ProjectCodeOrderByCreatedAtDesc(String projectCode, Pageable pageable);
}
