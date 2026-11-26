package com.construction.repository;

import com.construction.entity.ProgressUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgressUpdateRepository extends JpaRepository<ProgressUpdate, UUID> {
    Page<ProgressUpdate> findByProject_ProjectCodeOrderByCreatedAtDesc(String projectCode, Pageable pageable);
    Optional<ProgressUpdate> findFirstByProject_ProjectCodeOrderByCreatedAtDesc(String projectCode);
    List<ProgressUpdate> findByProject_ProjectCodeAndUpdateDateOrderByCreatedAtDesc(String projectCode, LocalDate updateDate);
    List<ProgressUpdate> findByProject_ProjectCodeAndUpdateDateBetweenOrderByUpdateDateDescCreatedAtDesc(String projectCode, LocalDate startDate, LocalDate endDate);
    List<ProgressUpdate> findByProject_ProjectCodeOrderByUpdateDateDescCreatedAtDesc(String projectCode);
}
