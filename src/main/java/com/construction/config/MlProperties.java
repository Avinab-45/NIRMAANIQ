package com.construction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the YOLO11 Python FastAPI ML microservice.
 */
@Configuration
@ConfigurationProperties(prefix = "app.ml")
@Getter
@Setter
public class MlProperties {

    /**
     * Whether the YOLO11 FastAPI ML microservice integration is enabled.
     */
    private boolean enabled = true;

    /**
     * Root URL of the Python FastAPI service (e.g. http://localhost:8000).
     */
    private String url = "http://localhost:8000";

    /**
     * Shared secret / API key for authenticating with FastAPI (optional).
     */
    private String apiKey = "";

    /**
     * Inference endpoint path, default /predict.
     */
    private String predictEndpoint = "/predict";

    /**
     * HTTP connection timeout in seconds.
     */
    private int connectTimeoutSeconds = 10;

    /**
     * HTTP read / inference execution timeout in seconds.
     */
    private int readTimeoutSeconds = 30;

    /**
     * Multipart form data parameter name for the uploaded image (default: file).
     */
    private String fileParamName = "file";

    public String getFullPredictUrl() {
        String cleanUrl = url != null ? url.replaceAll("/+$", "") : "http://localhost:8000";
        String cleanEndpoint = predictEndpoint != null ? predictEndpoint.trim() : "/predict";
        if (!cleanEndpoint.startsWith("/")) {
            cleanEndpoint = "/" + cleanEndpoint;
        }
        return cleanUrl + cleanEndpoint;
    }
}
