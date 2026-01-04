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

        ReflectionTestUtils.setField(uploadService, "authServiceUrl", "http://auth-service");

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

    @Test
    void processFile_ShouldUpload_WhenSafeAndWithinQuota() throws Exception {
        // Arrange
        String fileName = "test-document.pdf";
        byte[] pdfHeader = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D};
        InputStream fileStream = new ByteArrayInputStream(pdfHeader);

        when(redisBanService.isUserBanned(userId)).thenReturn(false);
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


        StorageQuotaExceededException exception = assertThrows(StorageQuotaExceededException.class, () ->
                uploadService.processFile(fileStream, fileName, userId, mockJwt)
        );

        assertTrue(exception.getMessage().contains("Storage quota exceeded") ||
                exception.getMessage().contains("quota"));

        verify(s3Service, never()).uploadFile(anyString(), any());
    }

    @Test
    void processFile_ShouldRejectAndBan_WhenUnsafeContentDetected() throws Exception {
        // Arrange
        String fileName = "virus.exe";
        InputStream fileStream = new ByteArrayInputStream("malware code".getBytes());

        when(redisBanService.isUserBanned(userId)).thenReturn(false);
        when(securityService.checkFileSecurity(any(), anyString(), anyString()))
                .thenReturn(Map.of("security_status", "unsafe", "rejection_reason", "Malware detected"));

        // Act & Assert

        BusinessException exception = assertThrows(BusinessException.class, () ->
                uploadService.processFile(fileStream, fileName, userId, mockJwt)
        );

        assertEquals("Security Policy Violation: Malware detected", exception.getMessage());

        verify(redisBanService).incrementViolationAndCheckBan(userId, userEmail);
        verify(s3Service, never()).uploadFile(anyString(), any());
    }


    @Test
    void processFile_ShouldFastFail_WhenUserIsBanned() {
        // Arrange
        when(redisBanService.isUserBanned(userId)).thenReturn(true);
        InputStream fileStream = new ByteArrayInputStream("data".getBytes());

        // Act & Assert

        BusinessException exception = assertThrows(BusinessException.class, () ->
                uploadService.processFile(fileStream, "any.txt", userId, mockJwt)
        );

        assertTrue(exception.getMessage().contains("Account suspended") ||
                exception.getMessage().contains("banned"));

        verifyNoInteractions(securityService);
        verifyNoInteractions(s3Service);
    }


    @Test
    void processFile_ShouldFallbackExtension_WhenTikaFails() throws Exception {

        String fileName = "holiday_video.mp4";

        byte[] binaryData = new byte[] {0, 1, 2, 3, 4, 5};
        InputStream fileStream = new ByteArrayInputStream(binaryData);

        when(redisBanService.isUserBanned(userId)).thenReturn(false);

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

        assertEquals("video", result.getFileType());

        verify(securityService).checkFileSecurity(any(), eq(fileName), eq("video"));
    }
}