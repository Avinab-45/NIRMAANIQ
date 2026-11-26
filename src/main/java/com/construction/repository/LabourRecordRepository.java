package com.construction.repository;

import com.construction.entity.LabourRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabourRecordRepository extends JpaRepository<LabourRecord, UUID> {
    Page<LabourRecord> findByProject_ProjectCodeOrderByWorkDateDesc(String projectCode, Pageable pageable);
    List<LabourRecord> findByProject_ProjectCode(String projectCode);
}
