package com.construction.service;

import com.construction.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupabaseStorageServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SupabaseStorageService storageService;

    private static final String SUPABASE_URL = "https://test-project.supabase.co";
    private static final String SERVICE_ROLE_KEY = "test-service-key-12345";
    private static final String STORAGE_BUCKET = "construction-media";

    @BeforeEach
    void setUp() {
        storageService = new SupabaseStorageService(SUPABASE_URL, SERVICE_ROLE_KEY, STORAGE_BUCKET, restTemplate);
    }

    @Test
    @DisplayName("Successful file upload to private bucket returns valid storage path")
    void uploadFile_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "site_plan.pdf", "application/pdf", "dummy PDF content".getBytes()
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"Key\":\"construction-media/PRJ-1/uuid.pdf\"}", HttpStatus.OK));

        String path = storageService.uploadFile("PRJ-1", file);

        assertNotNull(path);
        assertTrue(path.startsWith("PRJ-1/"));
        assertTrue(path.endsWith("-site_plan.pdf"));
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("Supabase Storage rejecting upload with 403 throws ApiException")
    void uploadFile_HttpError_ThrowsApiException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "blueprint.png", "image/png", "dummy image content".getBytes()
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden"));

        ApiException ex = assertThrows(ApiException.class, () -> storageService.uploadFile("PRJ-1", file));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().contains("upload failed"));
    }

    @Test
    @DisplayName("Supabase Storage network failure during upload throws ApiException")
    void uploadFile_NetworkFailure_ThrowsApiException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "image bytes".getBytes()
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        ApiException ex = assertThrows(ApiException.class, () -> storageService.uploadFile("PRJ-1", file));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertTrue(ex.getMessage().contains("Failed to upload file"));
    }

    @Test
    @DisplayName("Successful signed URL generation returns authenticated URL")
    void generateSignedUrl_Success() {
        String jsonResponse = "{\"signedURL\":\"/object/sign/construction-media/PRJ-1/photo.jpg?token=secret123\"}";

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        String signedUrl = storageService.generateSignedUrl("PRJ-1/photo.jpg");

        assertNotNull(signedUrl);
        assertTrue(signedUrl.contains("/storage/v1/object/sign/construction-media/PRJ-1/photo.jpg?token=secret123"));
        assertFalse(signedUrl.contains("/public/"));
    }

    @Test
    @DisplayName("Signed URL generation failure throws ApiException (never returns public URL)")
    void generateSignedUrl_Failure_ThrowsApiException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        ApiException ex = assertThrows(ApiException.class, () -> storageService.generateSignedUrl("PRJ-1/photo.jpg"));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().contains("signed URL generation failed"));
    }

    @Test
    @DisplayName("Successful file deletion from Supabase Storage")
    void deleteFile_Success() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertDoesNotThrow(() -> storageService.deleteFile("PRJ-1/photo.jpg"));
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("File deletion failure throws ApiException")
    void deleteFile_Failure_ThrowsApiException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        ApiException ex = assertThrows(ApiException.class, () -> storageService.deleteFile("PRJ-1/photo.jpg"));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
    }

    @Test
    @DisplayName("Missing Supabase Storage credentials throws ApiException")
    void storage_MissingCredentials_ThrowsApiException() {
        SupabaseStorageService unconfigured = new SupabaseStorageService("", "", "construction-media", restTemplate);
        MockMultipartFile file = new MockMultipartFile("f", "f.txt", "text/plain", "data".getBytes());

        assertThrows(ApiException.class, () -> unconfigured.uploadFile("PRJ-1", file));
        assertThrows(ApiException.class, () -> unconfigured.generateSignedUrl("PRJ-1/f.txt"));
        assertThrows(ApiException.class, () -> unconfigured.deleteFile("PRJ-1/f.txt"));
    }
}
