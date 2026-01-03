package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.S3UploadResult;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Exception.BusinessException;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Exception.StorageQuotaExceededException;
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
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private final Tika tika = new Tika();

    private static final long GIGABYTE = 1024 * 1024 * 1024;
    private static final String CONFIRMATION_KEY_PREFIX = "file:sync_confirm:";
    private static final long ASYNC_TIMEOUT_SECONDS = 90;
    private static final long POLL_INTERVAL_MS = 100;


    public ProcessedDocument processFile(InputStream fileStream, String fileName, String userId, Jwt token) {
        // 1. Pre-Check: Is User Banned?
        checkBanStatus(userId, fileName);

        Path tempFilePath = null;
        try {
            // 2. Persist to Temp File for Processing
            tempFilePath = createTempFile(fileStream, fileName);
            long fileSize = Files.size(tempFilePath);

            // 3. Security & File Type Validation
            String fileType = validateFileSecurity(tempFilePath, fileName, userId, token);

            // 4. Quota Check
            enforceStorageQuota(userId, fileSize, token);

            // 5. Upload & Process
            return uploadAndConfirm(tempFilePath, fileName, fileType, userId, fileSize, token);

        } catch (BusinessException | StorageQuotaExceededException e) {
            // Rethrow domain-specific exceptions as-is so the Controller/Test receives them directly
            throw e;
        } catch (Exception e) {
            // Wrap unexpected technical errors (IO, etc.)
            log.error("IO Error processing file: {}", fileName, e);
            throw new RuntimeException("Failed to process file upload", e);
        } finally {
            cleanupTempFile(tempFilePath);
        }
    }

    // --- Helper Methods ---

    private void checkBanStatus(String userId, String fileName) {
        if (redisBanService.isUserBanned(userId)) {
            log.warn("File {} rejected: User {} is banned.", fileName, userId);
            throw new BusinessException("Upload rejected: Account suspended due to policy violations.");
        }
    }

    private Path createTempFile(InputStream fileStream, String fileName) throws IOException {
        Path tempPath = Files.createTempFile("upload-", fileName);
        Files.copy(fileStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Temp file created at: {}", tempPath);
        return tempPath;
    }

    private String validateFileSecurity(Path filePath, String fileName, String userId, Jwt token) throws Exception {
        try (InputStream tikaStream = Files.newInputStream(filePath);
             InputStream securityStream = Files.newInputStream(filePath)) {

            String fileType = detectFileType(tikaStream, fileName);
            Map<String, Object> result = securityService.checkFileSecurity(securityStream, fileName, fileType);

            String status = (String) result.get("security_status");
            if ("unsafe".equalsIgnoreCase(status)) {
                String reason = (String) result.get("rejection_reason");
                log.warn("Security violation for file {}: {}", fileName, reason);
                redisBanService.incrementViolationAndCheckBan(userId, token.getSubject());
                throw new BusinessException("Security Policy Violation: " + reason);
            }
            if ("error".equalsIgnoreCase(status)) {
                throw new BusinessException("Security Check Failed: " + result.get("rejection_reason"));
            }
            return fileType;
        }
    }

    private void enforceStorageQuota(String userId, long newFileSize, Jwt token) {
        long currentUsage = s3Service.getUserFolderSize(userId);
        Plan userPlan = fetchUserPlanSafely(userId, token);
        long maxQuota = calculateQuota(userPlan);

        if (currentUsage + newFileSize > maxQuota) {
            throw new StorageQuotaExceededException(String.format(
                    "Storage quota exceeded. Limit: %.2f GB, Usage: %.2f GB, File: %.2f MB",
                    (double) maxQuota / GIGABYTE,
                    (double) currentUsage / GIGABYTE,
                    (double) newFileSize / (1024 * 1024)
            ));
        }
    }

    private Plan fetchUserPlanSafely(String userId, Jwt token) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");
        Supplier<Plan> planSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, () ->
                restClient.get()
                        .uri(authServiceUrl, userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getTokenValue())
                        .retrieve()
                        .body(Plan.class)
        );

        try {
            return planSupplier.get();
        } catch (Exception e) {
            log.warn("Auth Service unavailable. Defaulting to BASIC plan. Error: {}", e.getMessage());
            return Plan.BASIC;
        }
    }

    private long calculateQuota(Plan plan) {
        if (plan == null) return GIGABYTE; // Default fallback
        return switch (plan) {
            case PRO -> GIGABYTE * 1000;
            case TEAM -> GIGABYTE * 5000;
            case BASIC -> GIGABYTE * 100;
            default -> GIGABYTE;
        };
    }

    private ProcessedDocument uploadAndConfirm(Path filePath, String fileName, String fileType, String userId, long fileSize, Jwt token) throws IOException {
        try (InputStream s3Stream = Files.newInputStream(filePath)) {
            String s3Key = userId + "/" + fileName;

            // Upload
            S3UploadResult uploadResult = s3Service.uploadFile(s3Key, s3Stream);

            // Queue Metadata Processing
            queueService.publishMetadataRequest(fileName, fileType, uploadResult.fileUrl(), userId, fileSize, token.getSubject());

            // Wait for processing confirmation
            String confirmedFileId = waitForConfirmation(userId, fileName);

            return ProcessedDocument.builder()
                    .id(confirmedFileId)
                    .fileName(fileName)
                    .fileType(fileType)
                    .s3Location(uploadResult.fileUrl())
                    .fileSize(fileSize)
                    .userId(userId)
                    .securityStatus("safe")
                    .build();
        }
    }

    private String waitForConfirmation(String userId, String fileName) {
        String key = CONFIRMATION_KEY_PREFIX + userId + ":" + fileName;
        log.info("Polling for confirmation key: {}", key);

        long deadline = System.currentTimeMillis() + (ASYNC_TIMEOUT_SECONDS * 1000);

        while (System.currentTimeMillis() < deadline) {
            String fileId = stringRedisTemplate.opsForValue().get(key);
            if (fileId != null && !fileId.isBlank()) {
                stringRedisTemplate.delete(key);
                return fileId;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for file processing", e);
            }
        }

        log.warn("Processing confirmation timed out for file: {}", fileName);
        return null; // Or throw exception based on requirement
    }

    private void cleanupTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", path, e);
            }
        }
    }

    private String detectFileType(InputStream stream, String fileName) throws IOException {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        String mimeType = tika.detect(stream, metadata);

        // Simple mapping based on mime-type or extension
        if (mimeType.startsWith("text/") || Set.of("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document").contains(mimeType)) {
            return "text";
        }
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";

        // Extension fallback for code/specific types
        String lowerName = fileName.toLowerCase();
        if (lowerName.matches(".*\\.(java|py|js|ts|cpp|c|go|rs|html|css|json|xml|yaml|yml)$")) return "code";
        if (lowerName.matches(".*\\.(mp4|mkv|webm|mov|avi)$")) return "video";
        if (lowerName.matches(".*\\.(mp3|wav|m4a|aac)$")) return "audio";

        return "default";
    }
}