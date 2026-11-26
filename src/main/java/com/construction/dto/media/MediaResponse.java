package com.construction.dto.media;

import com.construction.entity.enums.MediaCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {
    private UUID id;
    private UUID progressUpdateId;
    private String fileName;
    private String storagePath;
    private String signedUrl;
    private Long fileSize;
    private String mimeType;
    private MediaCategory mediaCategory;
    private String caption;
    private UUID uploadedById;
    private String uploadedByName;
    private LocalDateTime createdAt;
}
