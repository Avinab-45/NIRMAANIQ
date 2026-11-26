package com.construction.repository;

import com.construction.entity.MaterialItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialItemRepository extends JpaRepository<MaterialItem, UUID> {
    List<MaterialItem> findByProject_ProjectCodeOrderByNameAsc(String projectCode);
    Optional<MaterialItem> findByProject_ProjectCodeAndItemCode(String projectCode, String itemCode);
    boolean existsByProject_IdAndItemCode(UUID projectId, String itemCode);
}
