package com.construction.repository;

import com.construction.entity.RiskPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskPredictionRepository extends JpaRepository<RiskPrediction, UUID> {
    List<RiskPrediction> findByProject_ProjectCodeOrderByGeneratedAtDesc(String projectCode);
}
