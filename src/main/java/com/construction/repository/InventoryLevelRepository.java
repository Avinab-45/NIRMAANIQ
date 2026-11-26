package com.construction.repository;

import com.construction.entity.InventoryLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, UUID> {
    List<InventoryLevel> findByProject_ProjectCode(String projectCode);
    Optional<InventoryLevel> findByProject_IdAndMaterialItem_Id(UUID projectId, UUID materialItemId);
    Optional<InventoryLevel> findByProject_ProjectCodeAndMaterialItem_ItemCode(String projectCode, String itemCode);
}
