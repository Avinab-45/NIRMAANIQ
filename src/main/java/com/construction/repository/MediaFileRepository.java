package com.construction.repository;

import com.construction.entity.MediaFile;
import com.construction.entity.enums.MediaCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByProject_ProjectCodeOrderByCreatedAtDesc(String projectCode);
    List<MediaFile> findByProject_ProjectCodeAndMediaCategoryOrderByCreatedAtDesc(String projectCode, MediaCategory mediaCategory);
    Optional<MediaFile> findByIdAndProject_ProjectCode(UUID id, String projectCode);
}
