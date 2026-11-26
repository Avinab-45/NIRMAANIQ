package com.construction.repository;

import com.construction.entity.WeatherContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WeatherContextRepository extends JpaRepository<WeatherContext, UUID> {
    List<WeatherContext> findByProject_ProjectCodeOrderByRecordedDateDesc(String projectCode);
}
