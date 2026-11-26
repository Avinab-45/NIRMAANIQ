package com.construction.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiVisionClient implements AiVisionClient {

    private final boolean aiEnabled;
    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public GeminiVisionClient(
            @Value("${app.ai.enabled:false}") boolean aiEnabled,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gemini-1.5-flash}") String model,
            RestTemplateBuilder restTemplateBuilder) {
        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        this.model = model != null ? model : "gemini-1.5-flash";
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    // Constructor for testing/mock injection
    public GeminiVisionClient(boolean aiEnabled, String apiKey, String model, RestTemplate restTemplate) {
        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        this.model = model != null ? model : "gemini-1.5-flash";
        this.restTemplate = restTemplate;
    }

    @Override
    public VisionAnalysisResult analyzeConstructionImage(byte[] imageBytes, String mimeType, String contextPrompt) {
        if (!aiEnabled || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI vision analysis engine is not configured or disabled (AI_PROVIDER_ENABLED=false or missing AI_PROVIDER_API_KEY).");
        }

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image content is empty; cannot perform vision analysis.");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String prompt = (contextPrompt != null && !contextPrompt.isBlank())
                ? contextPrompt
                : "Analyze this construction site photograph. Provide an objective assessment of visible construction activity. "
                + "Return ONLY a valid JSON object matching this schema: "
                + "{\"workCategory\": \"string (e.g. BRICKWORK, FOUNDATION, CONCRETE_SLAB, PLASTERING, FLOORING, ELECTRICAL, PLUMBING, PAINTING, OTHER)\", "
                + "\"estimatedCompletionPercentage\": number (0.0 to 100.0 based only on visible area in frame), "
                + "\"analysisSummary\": \"string detailed overview of observed construction state\", "
                + "\"detectedIssues\": [\"string list of visible safety, quality, or structural concerns\"], "
                + "\"recommendations\": [\"string list of suggested actions or inspections\"]}";

        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> inlineData = Map.of(
                "mime_type", mimeType != null ? mimeType : "image/jpeg",
                "data", base64Image
        );
        Map<String, Object> imagePart = Map.of("inline_data", inlineData);

        Map<String, Object> content = Map.of("parts", List.of(textPart, imagePart));
        Map<String, Object> generationConfig = Map.of("responseMimeType", "application/json");

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Gemini API returned status " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new RuntimeException("Gemini API returned no analysis candidates.");
            }

            String candidateText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
            JsonNode analysisJson = objectMapper.readTree(candidateText);

            String workCategory = analysisJson.path("workCategory").asText("GENERAL_CONSTRUCTION");
            BigDecimal completionPct = BigDecimal.valueOf(analysisJson.path("estimatedCompletionPercentage").asDouble(0.0));
            String summary = analysisJson.path("analysisSummary").asText("Site visual inspection completed.");

            String detectedIssues = analysisJson.has("detectedIssues")
                    ? objectMapper.writeValueAsString(analysisJson.get("detectedIssues"))
                    : "[]";

            String recommendations = analysisJson.has("recommendations")
                    ? objectMapper.writeValueAsString(analysisJson.get("recommendations"))
                    : "[]";

            return new VisionAnalysisResult(
                    "GEMINI",
                    model,
                    workCategory,
                    completionPct,
                    summary,
                    detectedIssues,
                    recommendations,
                    candidateText
            );
        } catch (Exception e) {
            log.error("AI vision analysis invocation failed: {}", e.getMessage());
            throw new RuntimeException("AI vision analysis failed: " + e.getMessage(), e);
        }
    }
}
