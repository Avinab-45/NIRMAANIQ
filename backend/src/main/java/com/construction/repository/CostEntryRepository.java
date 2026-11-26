package com.construction.repository;

import com.construction.entity.CostEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CostEntryRepository extends JpaRepository<CostEntry, UUID> {
    Page<CostEntry> findByProject_ProjectCodeOrderByExpenseDateDesc(String projectCode, Pageable pageable);
    List<CostEntry> findByProject_ProjectCode(String projectCode);
}
