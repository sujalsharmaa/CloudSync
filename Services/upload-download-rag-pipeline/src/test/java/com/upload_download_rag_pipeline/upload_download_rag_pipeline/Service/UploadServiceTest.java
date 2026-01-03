package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.S3UploadResult;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Exception.BusinessException;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Exception.StorageQuotaExceededException;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.Plan;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.ProcessedDocument;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock private SecurityService securityService;
    @Mock private S3Service s3Service;
    @Mock private QueueService queueService;
    @Mock private RestClient restClient;
    @Mock private RedisBanService redisBanService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock private CircuitBreaker circuitBreaker;

    // Mocking the RestClient chain
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private UploadService uploadService;

    private Jwt mockJwt;
    private final String userId = "123";
    private final String userEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        mockJwt = mock(Jwt.class);
        lenient().when(mockJwt.getSubject()).thenReturn(userEmail);
        lenient().when(mockJwt.getTokenValue()).thenReturn("dummy-token");

        // Inject config value
        ReflectionTestUtils.setField(uploadService, "authServiceUrl", "http://auth-service");

        // Mock CircuitBreaker behavior to execute the supplier immediately
        lenient().when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        lenient().when(circuitBreaker.decorateSupplier(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(circuitBreaker.executeSupplier(any(Supplier.class))).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
    }

    private void mockPlanFetch(Plan plan) {
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString(), (Object) any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.body(Plan.class)).thenReturn(plan);
    }

    /**
     * Test Case UP-01: Upload Safe File Within Quota
     */
    @Test
    void processFile_ShouldUpload_WhenSafeAndWithinQuota() throws Exception {
        // Arrange
        String fileName = "test-document.pdf";
        // Use realistic PDF header bytes to ensure Tika doesn't default to text
        byte[] pdfHeader = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D};
        InputStream fileStream = new ByteArrayInputStream(pdfHeader);

        when(redisBanService.isUserBanned(userId)).thenReturn(false);

        // Use anyString() for fileType to be robust against Tika changes
        when(securityService.checkFileSecurity(any(), anyString(), anyString()))
                .thenReturn(Map.of("security_status", "safe", "rejection_reason", "none"));

        mockPlanFetch(Plan.DEFAULT); // 1GB limit
        when(s3Service.getUserFolderSize(userId)).thenReturn(100L);

        when(s3Service.uploadFile(anyString(), any())).thenReturn(new S3UploadResult("http://s3.url/file", 100L));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("generated-file-id");

        // Act
        ProcessedDocument result = uploadService.processFile(fileStream, fileName, userId, mockJwt);

        // Assert
        assertEquals("safe", result.getSecurityStatus());
        assertNotNull(result.getId());
        verify(s3Service).uploadFile(contains(userId + "/" + fileName), any());
    }

    /**
     * Test Case UP-02: Block Upload Exceeding Plan Quota
     */
    /**
     * Test Case UP-02: Block Upload Exceeding Plan Quota
     */
    @Test
    void processFile_ShouldReject_WhenQuotaExceeded() throws Exception {
        // Arrange
        String fileName = "large-video.mp4";
        InputStream fileStream = new ByteArrayInputStream("heavy content".getBytes());

        when(redisBanService.isUserBanned(userId)).thenReturn(false);
        when(securityService.checkFileSecurity(any(), anyString(), anyString()))
                .thenReturn(Map.of("security_status", "safe"));

        mockPlanFetch(Plan.DEFAULT); // 1GB Limit
        long almostFull = (1024L * 1024L * 1024L) - 10;
        when(s3Service.getUserFolderSize(userId)).thenReturn(almostFull);

        // Act & Assert
        // CHANGED: Expect StorageQuotaExceededException instead of BusinessException
        StorageQuotaExceededException exception = assertThrows(StorageQuotaExceededException.class, () ->
                uploadService.processFile(fileStream, fileName, userId, mockJwt)
        );

        // Verify the message contains the expected error text
        assertTrue(exception.getMessage().contains("Storage quota exceeded") ||
                exception.getMessage().contains("quota"));

        verify(s3Service, never()).uploadFile(anyString(), any());
    }

    /**
     * Test Case UP-03: Enforce Content Policy (Security Violation)
     */
    @Test
    void processFile_ShouldRejectAndBan_WhenUnsafeContentDetected() throws Exception {
        // Arrange
        String fileName = "virus.exe";
        InputStream fileStream = new ByteArrayInputStream("malware code".getBytes());

        when(redisBanService.isUserBanned(userId)).thenReturn(false);
        when(securityService.checkFileSecurity(any(), anyString(), anyString()))
                .thenReturn(Map.of("security_status", "unsafe", "rejection_reason", "Malware detected"));

        // Act & Assert
        // Expect BusinessException as seen in your logs
        BusinessException exception = assertThrows(BusinessException.class, () ->
                uploadService.processFile(fileStream, fileName, userId, mockJwt)
        );

        assertEquals("Security Policy Violation: Malware detected", exception.getMessage());

        verify(redisBanService).incrementViolationAndCheckBan(userId, userEmail);
        verify(s3Service, never()).uploadFile(anyString(), any());
    }

    /**
     * Test Case UP-04: Reject Upload from Banned User
     */
    @Test
    void processFile_ShouldFastFail_WhenUserIsBanned() {
        // Arrange
        when(redisBanService.isUserBanned(userId)).thenReturn(true);
        InputStream fileStream = new ByteArrayInputStream("data".getBytes());

        // Act & Assert
        // Expect BusinessException immediately
        BusinessException exception = assertThrows(BusinessException.class, () ->
                uploadService.processFile(fileStream, "any.txt", userId, mockJwt)
        );

        assertTrue(exception.getMessage().contains("Account suspended") ||
                exception.getMessage().contains("banned"));

        verifyNoInteractions(securityService);
        verifyNoInteractions(s3Service);
    }

    /**
     * Test Case UP-06: MIME Type Fallback Logic
     */
    @Test
    void processFile_ShouldFallbackExtension_WhenTikaFails() throws Exception {
        // Arrange
        String fileName = "holiday_video.mp4";
        // Use random bytes so Tika (hopefully) detects binary/octet-stream, not "text"
        byte[] binaryData = new byte[] {0, 1, 2, 3, 4, 5};
        InputStream fileStream = new ByteArrayInputStream(binaryData);

        when(redisBanService.isUserBanned(userId)).thenReturn(false);

        // RELAXED STUBBING: Use anyString() for fileType to avoid "Strict stubbing argument mismatch"
        // Then verify the actual call later
        when(securityService.checkFileSecurity(any(), eq(fileName), anyString()))
                .thenReturn(Map.of("security_status", "safe"));

        mockPlanFetch(Plan.DEFAULT);
        when(s3Service.getUserFolderSize(userId)).thenReturn(0L);
        when(s3Service.uploadFile(anyString(), any())).thenReturn(new S3UploadResult("url", 100L));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("id");

        // Act
        ProcessedDocument result = uploadService.processFile(fileStream, fileName, userId, mockJwt);

        // Assert
        // Verify that "video" was eventually determined (either by Tika or Fallback)
        assertEquals("video", result.getFileType());

        // Verify specifically that Security Service was called with "video" (Fallback logic)
        verify(securityService).checkFileSecurity(any(), eq(fileName), eq("video"));
    }
}