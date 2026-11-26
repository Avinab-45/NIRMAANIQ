package com.construction.service;

import com.construction.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageService {

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String storageBucket;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SupabaseStorageService(
            @Value("${app.supabase.url:https://placeholder.supabase.co}") String supabaseUrl,
            @Value("${app.supabase.service-role-key:}") String serviceRoleKey,
            @Value("${app.supabase.storage-bucket:construction-media}") String storageBucket,
            RestTemplateBuilder restTemplateBuilder) {
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.trim() : "";
        this.serviceRoleKey = serviceRoleKey != null ? serviceRoleKey.trim() : "";
        this.storageBucket = storageBucket != null ? storageBucket.trim() : "construction-media";
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
    }

    // Constructor for testing
    public SupabaseStorageService(String supabaseUrl, String serviceRoleKey, String storageBucket, RestTemplate restTemplate) {
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.trim() : "";
        this.serviceRoleKey = serviceRoleKey != null ? serviceRoleKey.trim() : "";
        this.storageBucket = storageBucket != null ? storageBucket.trim() : "construction-media";
        this.restTemplate = restTemplate;
    }

    /**
     * Uploads a file to Supabase Storage private bucket using the backend service role key.
     * Throws ApiException if the upload fails, preventing invalid records in the database.
     *
     * @param projectCode Project identifier used as folder path prefix
     * @param file        Multipart file payload
     * @return relative storage path within the bucket
     */
    public String uploadFile(String projectCode, MultipartFile file) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()
                || supabaseUrl.contains("placeholder") || serviceRoleKey.contains("placeholder")) {
            log.error("Supabase Storage configuration is missing or placeholder");
            throw new ApiException("Supabase Storage credentials are not properly configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String cleanFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storagePath = projectCode + "/" + UUID.randomUUID() + "-" + cleanFilename;

        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl.replaceAll("/+$", ""), storageBucket, storagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE
        ));

        try {
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("File successfully uploaded to private Supabase Storage bucket: {}", storagePath);
                return storagePath;
            } else {
                log.error("Supabase Storage rejected upload with status: {}", response.getStatusCode());
                throw new ApiException("Supabase Storage rejected upload with status " + response.getStatusCode(), HttpStatus.BAD_GATEWAY);
            }
        } catch (HttpStatusCodeException e) {
            log.error("Supabase Storage returned HTTP error during upload: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException("Supabase Storage upload failed: " + e.getStatusCode(), HttpStatus.BAD_GATEWAY);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload file to Supabase Storage: {}", e.getMessage());
            throw new ApiException("Failed to upload file to Supabase Storage: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Downloads raw file bytes from private Supabase Storage bucket using service role key.
     * Used by AI vision analysis service.
     *
     * @param storagePath Relative file path within the storage bucket
     * @return byte[] raw file payload
     */
    public byte[] downloadFile(String storagePath) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()
                || supabaseUrl.contains("placeholder") || serviceRoleKey.contains("placeholder")) {
            log.error("Supabase Storage configuration is missing or placeholder");
            throw new ApiException("Supabase Storage credentials are not properly configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String downloadUrl = String.format("%s/storage/v1/object/authenticated/%s/%s", supabaseUrl.replaceAll("/+$", ""), storageBucket, storagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", serviceRoleKey);

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(downloadUrl, HttpMethod.GET, requestEntity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new ApiException("Supabase Storage rejected download with status " + response.getStatusCode(), HttpStatus.BAD_GATEWAY);
            }
        } catch (HttpStatusCodeException e) {
            log.error("Supabase Storage download failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException("Supabase Storage download failed: " + e.getStatusCode(), HttpStatus.BAD_GATEWAY);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to download file from Supabase Storage: {}", e.getMessage());
            throw new ApiException("Failed to download file from Supabase Storage: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generates a signed URL for private bucket access (valid for 3600 seconds = 1 hour).
     * Fails closed with ApiException on error; never falls back to public URLs on private buckets.
     *
     * @param storagePath Relative file path within the storage bucket
     * @return Authenticated signed URL
     */
    public String generateSignedUrl(String storagePath) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()
                || supabaseUrl.contains("placeholder") || serviceRoleKey.contains("placeholder")) {
            log.error("Supabase Storage configuration is missing or placeholder");
            throw new ApiException("Supabase Storage credentials are not properly configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String signUrl = String.format("%s/storage/v1/object/sign/%s/%s", supabaseUrl.replaceAll("/+$", ""), storageBucket, storagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("expiresIn", 3600);

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(signUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("signedURL")) {
                    String relativeUrl = root.get("signedURL").asText();
                    String baseUrl = supabaseUrl.replaceAll("/+$", "");
                    return baseUrl + "/storage/v1" + (relativeUrl.startsWith("/") ? relativeUrl : "/" + relativeUrl);
                }
            }
            log.error("Supabase Storage did not return a signedURL in response body");
            throw new ApiException("Failed to generate signed URL from Supabase Storage: missing signedURL", HttpStatus.BAD_GATEWAY);
        } catch (HttpStatusCodeException e) {
            log.error("Supabase Storage returned error generating signed URL: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException("Supabase Storage signed URL generation failed: " + e.getStatusCode(), HttpStatus.BAD_GATEWAY);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not generate signed URL for path {}: {}", storagePath, e.getMessage());
            throw new ApiException("Failed to generate signed URL: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes a file from Supabase Storage bucket.
     * Ensures delete failures are reported rather than falsely claiming success.
     *
     * @param storagePath Relative file path within the storage bucket
     */
    public void deleteFile(String storagePath) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceRoleKey == null || serviceRoleKey.isBlank()
                || supabaseUrl.contains("placeholder") || serviceRoleKey.contains("placeholder")) {
            log.error("Supabase Storage configuration is missing or placeholder");
            throw new ApiException("Supabase Storage credentials are not properly configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String deleteUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl.replaceAll("/+$", ""), storageBucket, storagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.set("apikey", serviceRoleKey);

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, requestEntity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to delete file from Supabase Storage: status {}", response.getStatusCode());
                throw new ApiException("Failed to delete file from Supabase Storage: status " + response.getStatusCode(), HttpStatus.BAD_GATEWAY);
            }
            log.info("File successfully deleted from Supabase Storage: {}", storagePath);
        } catch (HttpStatusCodeException e) {
            log.error("Supabase Storage delete failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException("Supabase Storage delete failed: " + e.getStatusCode(), HttpStatus.BAD_GATEWAY);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete file from Supabase Storage: {}", e.getMessage());
            throw new ApiException("Failed to delete file from Supabase Storage: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
