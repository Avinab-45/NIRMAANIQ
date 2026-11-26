package com.construction.repository;

import com.construction.entity.BudgetLine;
import com.construction.entity.enums.CostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetLineRepository extends JpaRepository<BudgetLine, UUID> {
    List<BudgetLine> findByProject_ProjectCodeOrderByCreatedAtAsc(String projectCode);
    List<BudgetLine> findByProject_ProjectCodeAndCategory(String projectCode, CostCategory category);
    Optional<BudgetLine> findByIdAndProject_ProjectCode(UUID id, String projectCode);
}
