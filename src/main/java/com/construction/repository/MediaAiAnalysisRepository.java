package com.construction.repository;

import com.construction.entity.MediaAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaAiAnalysisRepository extends JpaRepository<MediaAiAnalysis, UUID> {

    List<MediaAiAnalysis> findByMediaFile_IdOrderByCreatedAtDesc(UUID mediaFileId);

    List<MediaAiAnalysis> findByProject_ProjectCodeOrderByCreatedAtDesc(String projectCode);

    List<MediaAiAnalysis> findByProject_IdOrderByCreatedAtDesc(UUID projectId);

    Optional<MediaAiAnalysis> findByIdAndProject_ProjectCode(UUID id, String projectCode);
}
