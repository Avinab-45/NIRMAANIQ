package com.construction.controller;

import com.construction.dto.ai.MediaAiAnalysisResponse;
import com.construction.dto.common.ApiResponse;
import com.construction.dto.media.MediaResponse;
import com.construction.entity.enums.MediaCategory;
import com.construction.service.AiService;
import com.construction.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectCode}/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final AiService aiService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @PathVariable String projectCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) MediaCategory category,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "progressUpdateId", required = false) UUID progressUpdateId) {
        MediaResponse response = mediaService.uploadMedia(projectCode, file, category, caption, progressUpdateId);
        return new ResponseEntity<>(ApiResponse.success("Media file uploaded successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getMediaFiles(
            @PathVariable String projectCode,
            @RequestParam(value = "category", required = false) MediaCategory category) {
        List<MediaResponse> response = mediaService.getMediaFiles(projectCode, category);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMediaFile(
            @PathVariable String projectCode,
            @PathVariable UUID mediaId) {
        mediaService.deleteMediaFile(projectCode, mediaId);
        return ResponseEntity.ok(ApiResponse.success("Media file deleted successfully", null));
    }

    @PostMapping("/{mediaId}/analyze")
    public ResponseEntity<ApiResponse<MediaAiAnalysisResponse>> analyzeMediaFile(
            @PathVariable String projectCode,
            @PathVariable UUID mediaId,
            @RequestParam(value = "reanalyze", defaultValue = "false") boolean reanalyze) {
        MediaAiAnalysisResponse response = aiService.analyzeMediaFile(projectCode, mediaId, reanalyze);
        return ResponseEntity.ok(ApiResponse.success("Media file AI analysis processed", response));
    }

    @GetMapping("/{mediaId}/analyses")
    public ResponseEntity<ApiResponse<List<MediaAiAnalysisResponse>>> getMediaAnalyses(
            @PathVariable String projectCode,
            @PathVariable UUID mediaId) {
        List<MediaAiAnalysisResponse> response = aiService.getMediaAnalyses(projectCode, mediaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{mediaId}/analyses/latest")
    public ResponseEntity<ApiResponse<MediaAiAnalysisResponse>> getLatestMediaAnalysis(
            @PathVariable String projectCode,
            @PathVariable UUID mediaId) {
        MediaAiAnalysisResponse response = aiService.getLatestMediaAnalysis(projectCode, mediaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
