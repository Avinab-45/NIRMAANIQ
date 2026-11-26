package com.construction.service.ml;

import com.construction.config.MlProperties;
import com.construction.dto.ml.FastApiPredictResponse;
import com.construction.dto.ml.MlDetection;
import com.construction.exception.MlServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MlServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private MlProperties mlProperties;
    private ObjectMapper objectMapper;
    private MlServiceClient mlServiceClient;

    @BeforeEach
    void setUp() {
        mlProperties = new MlProperties();
        mlProperties.setEnabled(true);
        mlProperties.setUrl("http://localhost:8000");
        mlProperties.setApiKey("test-ml-secret-key");
        mlProperties.setPredictEndpoint("/predict");
        mlProperties.setConnectTimeoutSeconds(5);
        mlProperties.setReadTimeoutSeconds(30);

        mlServiceClient = new MlServiceClient(mlProperties, restTemplate);
    }

    @Test
    @DisplayName("Successful YOLO11 prediction returns parsed FastApiPredictResponse")
    void predict_Success() {
        String jsonResponse = """
                {
                    "model": "yolo11x-construction",
                    "version": "1.0",
                    "status": "COMPLETED",
                    "summary": "Concrete foundation inspection complete. Rebar grid visible.",
                    "work_category": "FOUNDATION",
                    "estimated_completion_percentage": 65.5,
                    "detections": [
                        {
                            "class_name": "rebar_grid",
                            "confidence": 0.94,
                            "box": {"x1": 120.5, "y1": 80.0, "x2": 450.0, "y2": 380.0}
                        },
                        {
                            "class_name": "concrete_formwork",
                            "confidence": 0.88,
                            "box": {"x1": 50.0, "y1": 60.0, "x2": 500.0, "y2": 400.0}
                        }
                    ],
                    "issues": ["Minor pooling of groundwater at east trench corner"],
                    "recommendations": ["Pump out water prior to concrete pour"]
                }
                """;

        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        when(restTemplate.exchange(eq("http://localhost:8000/predict"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        byte[] fakeImageBytes = "dummy-jpeg-content".getBytes(StandardCharsets.UTF_8);
        FastApiPredictResponse result = mlServiceClient.predict(
                fakeImageBytes,
                "site_photo.jpg",
                "image/jpeg",
                "PRJ-01",
                UUID.randomUUID()
        );

        assertNotNull(result);
        assertEquals("yolo11x-construction", result.getModel());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals("FOUNDATION", result.getWorkCategory());
        assertEquals(new BigDecimal("65.5"), result.getEstimatedCompletionPercentage());
        assertTrue(result.getSummary().contains("Concrete foundation inspection"));
        assertNotNull(result.getDetections());
        assertEquals(2, result.getDetections().size());

        MlDetection firstDetection = result.getDetections().get(0);
        assertEquals("rebar_grid", firstDetection.getClassName());
        assertEquals(0.94, firstDetection.getConfidence());

        assertEquals(1, result.getIssues().size());
        assertTrue(result.getIssues().get(0).contains("groundwater"));
        assertEquals(1, result.getRecommendations().size());
        assertTrue(result.getRecommendations().get(0).contains("Pump out"));
    }

    @Test
    @DisplayName("Sends authentication headers (X-API-Key and Authorization Bearer) when API key is configured")
    void predict_SendsAuthenticationHeaders() {
        String jsonResponse = "{\"status\":\"COMPLETED\",\"summary\":\"Detections found\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);

        mlServiceClient.predict("dummy".getBytes(), "photo.png", "image/png", "PRJ-01", null);

        verify(restTemplate).exchange(eq("http://localhost:8000/predict"), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        HttpEntity<?> entity = captor.getValue();
        HttpHeaders headers = entity.getHeaders();

        assertEquals("test-ml-secret-key", headers.getFirst("X-API-Key"));
        assertEquals("Bearer test-ml-secret-key", headers.getFirst(HttpHeaders.AUTHORIZATION));
        assertTrue(headers.getContentType().includes(MediaType.MULTIPART_FORM_DATA));
    }

    @Test
    @DisplayName("Uses custom multipart file parameter name when configured in MlProperties")
    @SuppressWarnings("unchecked")
    void predict_CustomFileParamName() {
        mlProperties.setFileParamName("image");
        String jsonResponse = "{\"status\":\"COMPLETED\",\"summary\":\"Detections found\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);

        mlServiceClient.predict("dummy".getBytes(), "photo.png", "image/png", "PRJ-01", null);

        verify(restTemplate).exchange(eq("http://localhost:8000/predict"), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        HttpEntity<?> entity = captor.getValue();
        MultiValueMap<String, Object> body = (MultiValueMap<String, Object>) entity.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("image"), "Multipart form data should contain custom param 'image'");
    }

    @Test
    @DisplayName("Throws IllegalStateException when ML service is disabled in configuration")
    void predict_Disabled_ThrowsIllegalStateException() {
        mlProperties.setEnabled(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                mlServiceClient.predict("dummy".getBytes(), "photo.jpg", "image/jpeg", "PRJ-01", null)
        );

        assertTrue(ex.getMessage().contains("disabled"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Throws MlServiceException when FastAPI returns HTTP 401 Unauthorized")
    void predict_Http401_ThrowsMlServiceException() {
        HttpClientErrorException unauthorized = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                HttpHeaders.EMPTY,
                "Invalid API Key".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(unauthorized);

        MlServiceException ex = assertThrows(MlServiceException.class, () ->
                mlServiceClient.predict("dummy".getBytes(), "photo.jpg", "image/jpeg", "PRJ-01", null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertTrue(ex.getMessage().contains("HTTP 401"));
    }

    @Test
    @DisplayName("Throws MlServiceException when FastAPI returns HTTP 500 Internal Server Error")
    void predict_Http500_ThrowsMlServiceException() {
        HttpServerErrorException serverError = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                "CUDA out of memory".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(serverError);

        MlServiceException ex = assertThrows(MlServiceException.class, () ->
                mlServiceClient.predict("dummy".getBytes(), "photo.jpg", "image/jpeg", "PRJ-01", null)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    @DisplayName("Throws MlServiceException on connection timeout or unreachable service")
    void predict_Timeout_ThrowsMlServiceException() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        MlServiceException ex = assertThrows(MlServiceException.class, () ->
                mlServiceClient.predict("dummy".getBytes(), "photo.jpg", "image/jpeg", "PRJ-01", null)
        );

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
        assertTrue(ex.getMessage().contains("timed out or is unavailable"));
    }

    @Test
    @DisplayName("Throws MlServiceException when response body is empty")
    void predict_EmptyResponseBody_ThrowsMlServiceException() {
        ResponseEntity<String> emptyResponse = new ResponseEntity<>("", HttpStatus.OK);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(emptyResponse);

        MlServiceException ex = assertThrows(MlServiceException.class, () ->
                mlServiceClient.predict("dummy".getBytes(), "photo.jpg", "image/jpeg", "PRJ-01", null)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().contains("empty response"));
    }
}
