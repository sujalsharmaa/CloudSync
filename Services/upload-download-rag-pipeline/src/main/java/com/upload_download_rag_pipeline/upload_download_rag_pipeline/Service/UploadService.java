package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.S3UploadResult;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.FileMetadataPostgres;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.Plan;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.ProcessedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.data.redis.core.StringRedisTemplate; // <-- NEW IMPORT

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit; // <-- NEW IMPORT

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final SecurityService securityService;
    private final S3Service s3Service;
    private final QueueService queueService;
    private final RestClient restClient;
    private final RedisBanService redisBanService;
    private final StringRedisTemplate stringRedisTemplate; // <-- NEW INJECTION for waiting
    private final Tika tika = new Tika();
    private static final long GIGABYTE = 1024 * 1024 * 1024;

    private static final String CONFIRMATION_KEY_PREFIX = "file:sync_confirm:";
    private static final long ASYNC_TIMEOUT_SECONDS = 20; // Max time to wait for metadata processing

    /**
     * Handles the initial file upload, security check, quota check, and S3 storage.
     */
    public ProcessedDocument processFile(InputStream fileStream, String fileName, String userId,Jwt token) {
        // --- PRE-CHECK: CHECK BAN STATUS ---
        if (redisBanService.isUserBanned(userId)) {
            log.warn("File {} rejected: User {} is currently banned.", fileName, userId);
            return ProcessedDocument.builder()
                    .fileName(fileName)
                    .securityStatus("banned")
                    .rejectionReason("User account is temporarily or permanently banned due to policy violations.")
                    .build();
        }
        // --- END PRE-CHECK ---

        Path tempFilePath = null;
        try {
            // 1. Write the incoming stream to a temporary file...
            tempFilePath = Files.createTempFile("upload-", fileName);
            long newFileSize = Files.copy(fileStream, tempFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to temporary path: {} with size: {} bytes", tempFilePath, newFileSize);

            // 2. Open multiple streams...
            try (InputStream tikaStream = Files.newInputStream(tempFilePath);
                 InputStream securityStream = Files.newInputStream(tempFilePath)) {

                // 3. Detect file type...
                String fileType = detectFileType(tikaStream, fileName);
                log.info("Detected file type: {} for file: {}", fileType, fileName);


                // 5. SYNCHRONOUS SECURITY CHECK
                Map<String, Object> securityCheckResult = securityService.checkFileSecurity(securityStream, fileName, fileType);
                String securityStatus = (String) securityCheckResult.get("security_status");
                String rejectionReason = (String) securityCheckResult.get("rejection_reason");

                // --- FIX: Split check for "unsafe" vs "error" ---

                // 5a. If it's a GENUINE "unsafe" violation, increment ban count
                if ("unsafe".equalsIgnoreCase(securityStatus)) {
                    log.warn("File {} rejected due to security policy. Reason: {}", fileName, rejectionReason);

                    // This is a real violation, so increment the counter
                    long violationCount = redisBanService.incrementViolationAndCheckBan(userId,token.getSubject());
                    log.warn("User {} policy violation count increased to {}.", userId, violationCount);

                    return ProcessedDocument.builder()
                            .fileName(fileName)
                            .fileType(fileType)
                            .securityStatus(securityStatus)
                            .rejectionReason(rejectionReason)
                            .build();
                }

                // 5b. If it's an INTERNAL error, reject the file but DO NOT ban
                else if ("error".equalsIgnoreCase(securityStatus)) {
                    log.error("File {} rejected due to internal security check error. Reason: {}", fileName, rejectionReason);

                    // DO NOT increment the ban counter. This is a system fault.
                    return ProcessedDocument.builder()
                            .fileName(fileName)
                            .fileType(fileType)
                            .securityStatus("rejected_internal_error") // Clear status for the client
                            .rejectionReason("File could not be processed due to an internal system error. Please try again later.")
                            .build();
                }
                // --- END FIX ---

                // --- 6. STORAGE QUOTA CHECK (Remains the same) ---
                long currentUsage = s3Service.getUserFolderSize(userId);
                long maxQuota = GIGABYTE; // Default to 1 GB

                Plan plan = restClient.get()
                        .uri("http://localhost:8080/api/auth/getStoragePlan/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getTokenValue())
                        .retrieve()
                        .body(Plan.class);
                switch (plan){
                    case DEFAULT -> maxQuota = GIGABYTE;
                    case BASIC -> maxQuota = GIGABYTE*100;
                    case PRO -> maxQuota = GIGABYTE*1000;
                    case TEAM -> maxQuota= GIGABYTE*5000;
                    case null -> maxQuota = GIGABYTE;
                }

                if (currentUsage + newFileSize > maxQuota) {
                    String quotaRejectionReason = String.format(
                            "Storage quota exceeded. Max quota: %.2f GB, current usage: %.2f GB, file size: %.2f MB.",
                            (double) maxQuota / (1024 * 1024 * 1024),
                            (double) currentUsage / (1024 * 1024 * 1024),
                            (double) newFileSize / (1024 * 1024)
                    );

                    log.warn("File {} rejected for user {} because storage quota would be exceeded.", fileName, userId);
                    return ProcessedDocument.builder()
                            .fileName(fileName)
                            .fileType(fileType)
                            .securityStatus("rejected")
                            .rejectionReason(quotaRejectionReason)
                            .build();
                }
                // --- END QUOTA CHECK ---


                // 7. If safe AND within quota, upload to S3...
                try (InputStream s3Stream = Files.newInputStream(tempFilePath)) {
                    String s3Key = userId + "/" + fileName; // Use user-specific folder
                    String confirmationKey = CONFIRMATION_KEY_PREFIX + userId + ":" + fileName; // Define confirmation key

                    S3UploadResult s3UploadResult = s3Service.uploadFile(s3Key, s3Stream);
                    log.info("File {} successfully uploaded to S3 at: {}", fileName, s3UploadResult.fileUrl());

                    // --- OLD: NEW STEP: SAVE PENDING FILE MARKER TO REDIS ---
                    //redisFileService.savePendingFileMarker(userId, s3Key);

                    // 8. ASYNCHRONOUSLY trigger the metadata processing service
                    queueService.publishMetadataRequest(fileName, fileType, s3UploadResult.fileUrl(), userId, newFileSize, token.getSubject());

                    // --- NEW: BLOCKING WAIT FOR CONFIRMATION FROM CONSUMER ---
                    log.info("Waiting for metadata processing confirmation on key: {}", confirmationKey);

                    long startTime = System.currentTimeMillis();
                    boolean confirmed = false;
                    String confirmedFileId = null;

                    // Poll Redis for the confirmation key with a timeout
                    while ((System.currentTimeMillis() - startTime) < ASYNC_TIMEOUT_SECONDS * 1000) {
                        // FIX: Read the value from the key
                        confirmedFileId = stringRedisTemplate.opsForValue().get(confirmationKey);

                        // FIX: Check if the retrieved ID is valid
                        if (confirmedFileId != null && !confirmedFileId.isEmpty()) {
                            confirmed = true;
                            break;
                        }
                        // Wait briefly before polling again
                        TimeUnit.MILLISECONDS.sleep(500);
                    }

                    // Cleanup the confirmation key immediately
                    stringRedisTemplate.delete(confirmationKey);
                    // --- END BLOCKING WAIT ---

                    if (!confirmed) {
                        // Decide how to handle timeout (e.g., return a warning status or error)
                        log.warn("File {} metadata processing timed out after {} seconds. Returning partial success/warning.", fileName, ASYNC_TIMEOUT_SECONDS);
                    } else {
                        log.info("File {} metadata processing confirmed by consumer.", fileName);
                    }


                    // 9. Return a success response to the user
                    return ProcessedDocument.builder()
                            .id(confirmedFileId)
                            .fileName(fileName)
                            .fileType(fileType)
                            .s3Location(s3UploadResult.fileUrl())
                            .fileSize(newFileSize)
                            .userId(userId)
                            .securityStatus("safe")
                            // Note: You could add a status field like 'metadataStatus: confirmed/pending' here
                            .build();
                } // s3Stream closed here

            } // tikaStream and securityStream closed here
        } catch (Exception e) {
            log.error("Error processing file: {}", fileName, e);
            throw new RuntimeException("Failed to process file", e);
        } finally {
            // 10. CRITICAL: Ensure the temporary file is deleted after processing
            if (tempFilePath != null) {
                try {
                    Files.deleteIfExists(tempFilePath);
                    log.info("Temporary file deleted: {}", tempFilePath);
                } catch (IOException e) {
                    log.error("Failed to delete temporary file: {}", tempFilePath, e);
                }
            }
        }
    }

    private String detectFileType(InputStream stream, String fileName) throws Exception {
        // ... (detectFileType method remains the same) ...
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
        return "default";
    }
}
