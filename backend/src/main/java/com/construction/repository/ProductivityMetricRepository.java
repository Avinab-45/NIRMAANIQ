package com.construction.repository;

import com.construction.entity.ProductivityMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductivityMetricRepository extends JpaRepository<ProductivityMetric, UUID> {
    List<ProductivityMetric> findByProject_ProjectCodeOrderByMetricDateDesc(String projectCode);
}
