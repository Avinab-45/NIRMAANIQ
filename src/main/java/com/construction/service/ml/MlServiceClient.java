package com.construction.service.ml;

import com.construction.config.MlProperties;
import com.construction.dto.ml.FastApiPredictResponse;
import com.construction.dto.ml.MlDetection;
import com.construction.exception.MlServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HTTP Client connecting the Spring Boot backend to the external Python FastAPI YOLO11 inference microservice.
 * Sends construction site photographs as multipart/form-data and receives model detections.
 */
@Slf4j
@Component
public class MlServiceClient {

    private final MlProperties mlProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public MlServiceClient(MlProperties mlProperties, RestTemplateBuilder restTemplateBuilder) {
        this.mlProperties = mlProperties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(mlProperties.getConnectTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(mlProperties.getReadTimeoutSeconds()))
                .build();
    }

    // Testing constructor
    public MlServiceClient(MlProperties mlProperties, RestTemplate restTemplate) {
        this.mlProperties = mlProperties;
        this.restTemplate = restTemplate;
    }

    /**
     * Sends a construction site photograph to the Python FastAPI YOLO11 model endpoint (/predict).
     *
     * @param imageBytes  Raw photograph bytes downloaded securely from private Supabase Storage
     * @param filename    Original filename for Content-Disposition
     * @param mimeType    Image MIME type (e.g. image/jpeg, image/png)
     * @param projectCode Associated project code
     * @param mediaId     Associated media UUID
     * @return FastApiPredictResponse parsed model detections and analysis
     */
    public FastApiPredictResponse predict(byte[] imageBytes, String filename, String mimeType, String projectCode, UUID mediaId) {
        if (!mlProperties.isEnabled()) {
            throw new IllegalStateException("YOLO11 ML inference service is disabled (ML_SERVICE_ENABLED=false)");
        }

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Cannot send empty photograph bytes for ML inference");
        }

        String predictUrl = mlProperties.getFullPredictUrl();
        log.info("Sending photo {} ({} bytes) to YOLO11 FastAPI service at {}", mediaId, imageBytes.length, predictUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // Inject shared API key if configured
        if (mlProperties.getApiKey() != null && !mlProperties.getApiKey().isBlank()) {
            String key = mlProperties.getApiKey().trim();
            headers.set("X-API-Key", key);
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + key);
        }

        // Prepare multipart body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Custom ByteArrayResource with non-null getFilename() required by Spring's FormHttpMessageConverter
        ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return (filename != null && !filename.isBlank()) ? filename : "construction_photo.jpg";
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        if (mimeType != null && !mimeType.isBlank()) {
            try {
                fileHeaders.setContentType(MediaType.parseMediaType(mimeType));
            } catch (Exception e) {
                fileHeaders.setContentType(MediaType.IMAGE_JPEG);
            }
        } else {
            fileHeaders.setContentType(MediaType.IMAGE_JPEG);
        }

        HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(imageResource, fileHeaders);
        String paramName = (mlProperties.getFileParamName() != null && !mlProperties.getFileParamName().isBlank())
                ? mlProperties.getFileParamName().trim()
                : "file";
        body.add(paramName, fileEntity);

        // Optional metadata parts
        if (projectCode != null && !projectCode.isBlank()) {
            body.add("project_code", projectCode);
        }
        if (mediaId != null) {
            body.add("media_id", mediaId.toString());
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(predictUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isBlank()) {
                String responseBody = response.getBody();
                log.info("Received successful prediction response from YOLO11 service for media {}", mediaId);

                FastApiPredictResponse predictResponse;
                try {
                    predictResponse = objectMapper.readValue(responseBody, FastApiPredictResponse.class);
                } catch (Exception parseEx) {
                    log.error("Failed to parse YOLO11 response JSON: {}", parseEx.getMessage());
                    throw new MlServiceException("Invalid JSON response from YOLO11 service: " + parseEx.getMessage(), parseEx);
                }

                // Preserve full raw response string for training/audit
                predictResponse.setRawJson(responseBody);

                // Fallback default model name if omitted
                if (predictResponse.getModel() == null || predictResponse.getModel().isBlank()) {
                    predictResponse.setModel("yolo11");
                }

                // Synthesize summary if not explicitly provided by the model
                if (predictResponse.getSummary() == null || predictResponse.getSummary().isBlank()) {
                    predictResponse.setSummary(generateSummary(predictResponse));
                }

                return predictResponse;
            } else {
                log.error("YOLO11 service returned unexpected status or empty response: {}", response.getStatusCode());
                throw new MlServiceException("YOLO11 service returned empty response or status " + response.getStatusCode(), HttpStatus.BAD_GATEWAY);
            }
        } catch (ResourceAccessException e) {
            log.error("Network or timeout error contacting YOLO11 FastAPI service at {}: {}", predictUrl, e.getMessage());
            throw new MlServiceException("YOLO11 ML service connection timed out or is unavailable: " + e.getMessage(), HttpStatus.GATEWAY_TIMEOUT, e);
        } catch (HttpStatusCodeException e) {
            log.error("YOLO11 FastAPI service returned error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            HttpStatus httpStatus = HttpStatus.resolve(e.getStatusCode().value());
            if (httpStatus == null) httpStatus = HttpStatus.BAD_GATEWAY;
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new MlServiceException("Authentication failed with YOLO11 ML service: HTTP " + e.getStatusCode().value(), httpStatus, e);
            }
            throw new MlServiceException("YOLO11 ML inference failed with HTTP " + e.getStatusCode().value(), httpStatus, e);
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during YOLO11 inference call: {}", e.getMessage());
            throw new MlServiceException("Failed to execute YOLO11 model inference: " + e.getMessage(), e);
        }
    }

    private String generateSummary(FastApiPredictResponse resp) {
        if (resp.getDetections() != null && !resp.getDetections().isEmpty()) {
            Map<String, Long> counts = resp.getDetections().stream()
                    .filter(d -> d.getClassName() != null && !d.getClassName().isBlank())
                    .collect(Collectors.groupingBy(MlDetection::getClassName, Collectors.counting()));

            String details = counts.entrySet().stream()
                    .map(e -> e.getValue() + " " + e.getKey())
                    .collect(Collectors.joining(", "));

            return "YOLO11 detected " + resp.getDetections().size() + " objects (" + details + ")";
        }
        return "YOLO11 inference completed with no detected construction objects.";
    }
}
