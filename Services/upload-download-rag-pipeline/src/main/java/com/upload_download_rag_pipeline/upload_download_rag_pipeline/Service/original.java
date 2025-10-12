//package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;
//
//import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.S3UploadResult;
//import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.Plan;
//import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.ProcessedDocument;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.tika.Tika;
//import org.apache.tika.metadata.Metadata;
//import org.apache.tika.metadata.TikaCoreProperties;
//import org.springframework.http.HttpHeaders;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestClient;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Map;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class UploadService {
//
//    private final SecurityService securityService;
//    private final S3Service s3Service;
//    private final QueueService queueService;
//    private final RestClient restClient;
//    private final Tika tika = new Tika();
//    private static final long GIGABYTE = 1024 * 1024 * 1024;
//
//    /**
//     * Handles the initial file upload, security check, quota check, and S3 storage.
//     */
//    public ProcessedDocument processFile(InputStream fileStream, String fileName, String userId,Jwt token) {
//        Path tempFilePath = null;
//        try {
//            // 1. Write the incoming stream to a temporary file instead of an in-memory byte array
//            tempFilePath = Files.createTempFile("upload-", fileName);
//            long newFileSize = Files.copy(fileStream, tempFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
//            log.info("File saved to temporary path: {} with size: {} bytes", tempFilePath, newFileSize);
//
//            // 2. Open multiple streams from the temporary file for subsequent steps
//            try (InputStream tikaStream = Files.newInputStream(tempFilePath);
//                 InputStream securityStream = Files.newInputStream(tempFilePath)) {
//
//                // 3. Detect file type (using stream from temp file)
//                String fileType = detectFileType(tikaStream, fileName);
//                log.info("Detected file type: {} for file: {}", fileType, fileName);
//
//                // 4. SYNCHRONOUS SECURITY CHECK (using stream from temp file)
//                Map<String, Object> securityCheckResult = securityService.checkFileSecurity(securityStream, fileName, fileType);
//                String securityStatus = (String) securityCheckResult.get("security_status");
//                String rejectionReason = (String) securityCheckResult.get("rejection_reason");
//
//                // 5. If unsafe, return immediately with a rejection status
//                if ("unsafe".equalsIgnoreCase(securityStatus)) {
//                    log.warn("File {} rejected due to security policy. Reason: {}", fileName, rejectionReason);
//                    return ProcessedDocument.builder()
//                            .fileName(fileName)
//                            .fileType(fileType)
//                            .securityStatus(securityStatus)
//                            .rejectionReason(rejectionReason)
//                            .build();
//                }
//
//                // --- 6. STORAGE QUOTA CHECK ---
//                long currentUsage = s3Service.getUserFolderSize(userId);
//                long maxQuota = GIGABYTE; // Default to 1 GB
//
//                Plan plan = restClient.get()
//                        .uri("http://localhost:8080/api/auth/getStoragePlan/{id}", userId)
//                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getTokenValue())
//                        .retrieve()
//                        .body(Plan.class);
//                switch (plan){
//                    case DEFAULT -> maxQuota = GIGABYTE;
//                    case BASIC -> maxQuota = GIGABYTE*100;
//                    case PRO -> maxQuota = GIGABYTE*1000;
//                    case TEAM -> maxQuota= GIGABYTE*5000;
//                    case null -> maxQuota = GIGABYTE;
//                }
//
//                if (currentUsage + newFileSize > maxQuota) {
//                    String quotaRejectionReason = String.format(
//                            "Storage quota exceeded. Max quota: %.2f GB, current usage: %.2f GB, file size: %.2f MB.",
//                            (double) maxQuota / (1024 * 1024 * 1024),
//                            (double) currentUsage / (1024 * 1024 * 1024),
//                            (double) newFileSize / (1024 * 1024)
//                    );
//
//                    log.warn("File {} rejected for user {} because storage quota would be exceeded.", fileName, userId);
//                    return ProcessedDocument.builder()
//                            .fileName(fileName)
//                            .fileType(fileType)
//                            .securityStatus("rejected")
//                            .rejectionReason(quotaRejectionReason)
//                            .build();
//                }
//                // --- END QUOTA CHECK ---
//
//
//                // 7. If safe AND within quota, upload to S3 (use stream from temp file)
//                try (InputStream s3Stream = Files.newInputStream(tempFilePath)) {
//                    String s3Key = userId + "/" + fileName; // Use user-specific folder
//
//                    S3UploadResult s3UploadResult = s3Service.uploadFile(s3Key, s3Stream);
//                    log.info("File {} successfully uploaded to S3 at: {}", fileName, s3UploadResult.fileUrl());
//
//                    // 8. ASYNCHRONOUSLY trigger the metadata processing service
//                    // Note: If s3Service.uploadFile already computes file size, use it.
//                    // Since we used Files.copy, we already have the size in 'newFileSize'.
//                    queueService.publishMetadataRequest(fileName, fileType, s3UploadResult.fileUrl(), userId, newFileSize, token.getClaims().get("email").toString());
//
//                    // 9. Return a success response to the user
//                    return ProcessedDocument.builder()
//                            .fileName(fileName)
//                            .fileType(fileType)
//                            .s3Location(s3UploadResult.fileUrl())
//                            .fileSize(newFileSize)
//                            .userId(userId)
//                            .securityStatus("safe")
//                            .build();
//                } // s3Stream closed here
//
//            } // tikaStream and securityStream closed here
//        } catch (Exception e) {
//            log.error("Error processing file: {}", fileName, e);
//            throw new RuntimeException("Failed to process file", e);
//        } finally {
//            // 10. CRITICAL: Ensure the temporary file is deleted after processing
//            if (tempFilePath != null) {
//                try {
//                    Files.deleteIfExists(tempFilePath);
//                    log.info("Temporary file deleted: {}", tempFilePath);
//                } catch (IOException e) {
//                    log.error("Failed to delete temporary file: {}", tempFilePath, e);
//                }
//            }
//        }
//    }
//
//    private String detectFileType(InputStream stream, String fileName) throws Exception {
//        Metadata metadata = new Metadata();
//        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
//        String mimeType = tika.detect(stream, metadata);
//
//        if (mimeType.startsWith("text/") || mimeType.contains("pdf") ||
//                mimeType.contains("document") || mimeType.contains("word")) {
//            return "text";
//        } else if (mimeType.contains("image/")) {
//            return "image";
//        } else if (mimeType.contains("code") || fileName.matches(".*\\.(java|py|js|ts|cpp|c|go|rs)$")) {
//            return "code";
//        } else if (mimeType.contains("video/")) {
//            return "video";
//        } else if (mimeType.contains("audio/")) {
//            return "audio";
//        } else if (mimeType.contains("spreadsheet") || mimeType.contains("excel")) {
//            return "spreadsheet";
//        }
//        return "default";
//    }
//
//    // Removed the problematic readInputStreamToBytes method.
//    // The functionality is replaced by Files.copy(fileStream, tempFilePath).
//}
