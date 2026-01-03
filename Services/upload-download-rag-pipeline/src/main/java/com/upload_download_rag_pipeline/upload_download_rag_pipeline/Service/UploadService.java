package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.S3UploadResult;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Exception.StorageQuotaExceededException;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Exception.BusinessException;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.Plan;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.ProcessedDocument;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    @Value("${authServiceUrl}")
    private String authServiceUrl;

    private final SecurityService securityService;
    private final S3Service s3Service;
    private final QueueService queueService;
    private final RestClient restClient;
    private final RedisBanService redisBanService;
    private final StringRedisTemplate stringRedisTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry; // Injected
    private final Tika tika = new Tika();
    private static final long GIGABYTE = 1024 * 1024 * 1024;

    private static final String CONFIRMATION_KEY_PREFIX = "file:sync_confirm:";
    private static final long POLL_INTERVAL_MS = 100;
    private static final long ASYNC_TIMEOUT_SECONDS = 90;

    public ProcessedDocument processFile(InputStream fileStream, String fileName, String userId, Jwt token) {
        // --- PRE-CHECK: CHECK BAN STATUS ---
        if (redisBanService.isUserBanned(userId)) {
            log.warn("File {} rejected: User {} is currently banned.", fileName, userId);
            // You can optionally throw an AccessDeniedException here if you want a 403 response
            // throw new org.springframework.security.access.AccessDeniedException("User is banned");

            return ProcessedDocument.builder()
                    .fileName(fileName)
                    .securityStatus("banned")
                    .rejectionReason("User account is temporarily or permanently banned due to policy violations.")
                    .build();
        }

        Path tempFilePath = null;
        try {
            tempFilePath = Files.createTempFile("upload-", fileName);
            long newFileSize = Files.copy(fileStream, tempFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to temporary path: {} with size: {} bytes", tempFilePath, newFileSize);

            try (InputStream tikaStream = Files.newInputStream(tempFilePath);
                 InputStream securityStream = Files.newInputStream(tempFilePath)) {

                String fileType = detectFileType(tikaStream, fileName);

                Map<String, Object> securityCheckResult = securityService.checkFileSecurity(securityStream, fileName, fileType);
                String securityStatus = (String) securityCheckResult.get("security_status");
                String rejectionReason = (String) securityCheckResult.get("rejection_reason");

                if ("unsafe".equalsIgnoreCase(securityStatus)) {
                    log.warn("File {} rejected due to security policy. Reason: {}", fileName, rejectionReason);
                    redisBanService.incrementViolationAndCheckBan(userId, token.getSubject());

                    // Throwing BusinessException ensures a 400 Bad Request with the reason
                    throw new BusinessException("Security Policy Violation: " + rejectionReason);
                } else if ("error".equalsIgnoreCase(securityStatus)) {
                    throw new BusinessException("Internal Security Check Error: " + rejectionReason);
                }

                // --- 5. STORAGE QUOTA CHECK REFACTORED (WITH RESILIENCE4J) ---
                long currentUsage = s3Service.getUserFolderSize(userId);
                long maxQuota = GIGABYTE;

                // Wrap RestClient call in Circuit Breaker
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");
                Supplier<Plan> planSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, () ->
                        restClient.get()
                                .uri(authServiceUrl, userId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getTokenValue())
                                .retrieve()
                                .body(Plan.class)
                );

                Plan plan;
                try {
                    plan = planSupplier.get();
                } catch (Exception e) {
                    log.warn("Circuit Breaker: Auth Service unavailable or failing. Defaulting to BASIC plan. Error: {}", e.getMessage());
                    // Fallback: If auth service is down, we cannot verify premium plans.
                    // Default to BASIC to be safe, or throw error depending on business logic.
                    // Here we assume BASIC to avoid blocking uploads if possible, or handle strictly.
                    // For now, we will treat it as null/default in the switch below.
                    plan = Plan.BASIC;
                }

                if (plan != null) {
                    switch (plan) {
                        case BASIC -> maxQuota = GIGABYTE * 100;
                        case PRO -> maxQuota = GIGABYTE * 1000;
                        case TEAM -> maxQuota = GIGABYTE * 5000;
                    }
                }

                if (currentUsage + newFileSize > maxQuota) {
                    String quotaRejectionReason = String.format(
                            "Storage quota exceeded. Max quota: %.2f GB, current usage: %.2f GB, file size: %.2f MB.",
                            (double) maxQuota / (1024 * 1024 * 1024),
                            (double) currentUsage / (1024 * 1024 * 1024),
                            (double) newFileSize / (1024 * 1024)
                    );
                    log.warn("File {} rejected for user {} because storage quota would be exceeded.", fileName, userId);

                    // THROWING THE CUSTOM EXCEPTION HERE
                    throw new StorageQuotaExceededException(quotaRejectionReason);
                }
                // --- END QUOTA CHECK ---

                try (InputStream s3Stream = Files.newInputStream(tempFilePath)) {
                    String s3Key = userId + "/" + fileName;
                    String confirmationKey = CONFIRMATION_KEY_PREFIX + userId + ":" + fileName;

                    S3UploadResult s3UploadResult = s3Service.uploadFile(s3Key, s3Stream);
                    queueService.publishMetadataRequest(fileName, fileType, s3UploadResult.fileUrl(), userId, newFileSize, token.getSubject());

                    log.info("Waiting for metadata processing confirmation on key: {}", confirmationKey);
                    long startTime = System.currentTimeMillis();
                    String confirmedFileId = null;

                    while ((System.currentTimeMillis() - startTime) < ASYNC_TIMEOUT_SECONDS * 1000) {
                        confirmedFileId = stringRedisTemplate.opsForValue().get(confirmationKey);
                        if (confirmedFileId != null && !confirmedFileId.isEmpty()) {
                            break;
                        }
                        try {
                            Thread.sleep(POLL_INTERVAL_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    stringRedisTemplate.delete(confirmationKey);

                    if (confirmedFileId == null || confirmedFileId.isEmpty()) {
                        log.warn("File {} metadata processing timed out.", fileName);
                        // Optional: throw new BusinessException("Processing timed out");
                        confirmedFileId = null;
                    }

                    return ProcessedDocument.builder()
                            .id(confirmedFileId)
                            .fileName(fileName)
                            .fileType(fileType)
                            .s3Location(s3UploadResult.fileUrl())
                            .fileSize(newFileSize)
                            .userId(userId)
                            .securityStatus("safe")
                            .build();
                }

            }
        } catch (StorageQuotaExceededException | BusinessException e) {
            // Re-throw these so the GlobalExceptionHandler catches them
            throw e;
        } catch (Exception e) {
            log.error("Error processing file: {}", fileName, e);
            throw new RuntimeException("Failed to process file", e);
        } finally {
            if (tempFilePath != null) {
                try {
                    Files.deleteIfExists(tempFilePath);
                } catch (IOException e) {
                    log.error("Failed to delete temporary file", e);
                }
            }
        }
    }

    private String detectFileType(InputStream stream, String fileName) throws Exception {
        // ... existing implementation ...
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        String mimeType = tika.detect(stream, metadata);

        if (mimeType.startsWith("text/") || mimeType.contains("pdf") ||
                mimeType.contains("document") || mimeType.contains("word")) {
            return "text";
        } else if (mimeType.contains("image/")) {
            return "image";
        } else if (mimeType.contains("code") || fileName.matches(".*\\.(java|py|js|ts|cpp|c|go|rs)$")) {
            return "code";
        } else if (mimeType.contains("video/")) {
            return "video";
        } else if (mimeType.contains("audio/")) {
            return "audio";
        } else if (mimeType.contains("spreadsheet") || mimeType.contains("excel")) {
            return "spreadsheet";
        }

        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".mp4") || lowerCaseFileName.endsWith(".mkv") || lowerCaseFileName.endsWith(".webm") || lowerCaseFileName.endsWith(".mov") || lowerCaseFileName.endsWith(".avi")) {
            return "video";
        }
        if (lowerCaseFileName.endsWith(".mp3") || lowerCaseFileName.endsWith(".wav") || lowerCaseFileName.endsWith(".m4a") || lowerCaseFileName.endsWith(".aac")) {
            return "audio";
        }

        return "default";
    }
}