package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.S3UploadResult;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.ProcessedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final SecurityService securityService;
    private final S3Service s3Service;
    private final QueueService queueService;
    private final Tika tika = new Tika();

    /**
     * Handles the initial file upload, security check, and S3 storage.
     * The metadata generation is handled asynchronously by a different service.
     */
    public ProcessedDocument processFile(InputStream fileStream, String fileName, String userId) {
        try {
            // 1. Read the file into a byte array for multiple uses
            byte[] fileBytes = readInputStreamToBytes(fileStream);
            InputStream firstStream = new java.io.ByteArrayInputStream(fileBytes);
            InputStream secondStream = new java.io.ByteArrayInputStream(fileBytes);

            // 2. Detect file type
            String fileType = detectFileType(firstStream, fileName);
            log.info("Detected file type: {} for file: {}", fileType, fileName);

            // 3. SYNCHRONOUS SECURITY CHECK
            Map<String, Object> securityCheckResult = securityService.checkFileSecurity(secondStream, fileName, fileType);
            String securityStatus = (String) securityCheckResult.get("security_status");
            String rejectionReason = (String) securityCheckResult.get("rejection_reason");

            // 4. If unsafe, return immediately with a rejection status
            if ("unsafe".equalsIgnoreCase(securityStatus)) {
                log.warn("File {} rejected due to security policy. Reason: {}", fileName, rejectionReason);
                return ProcessedDocument.builder()
                        .fileName(fileName)
                        .fileType(fileType)
                        .securityStatus(securityStatus)
                        .rejectionReason(rejectionReason)
                        .build();
            }

            // 5. If safe, upload to S3
            InputStream s3Stream = new java.io.ByteArrayInputStream(fileBytes);
            S3UploadResult s3UploadResult = s3Service.uploadFile(fileName, s3Stream);
            log.info("File {} successfully uploaded to S3 at: {}", fileName, s3UploadResult.fileUrl());

            // 6. ASYNCHRONOUSLY trigger the metadata processing service
            queueService.publishMetadataRequest(fileName, fileType, s3UploadResult.fileUrl(), userId, s3UploadResult.fileSize());

            // 7. Return a success response to the user
            return ProcessedDocument.builder()
                    .fileName(fileName)
                    .fileType(fileType)
                    .s3Location(s3UploadResult.fileUrl())
                    .fileSize(s3UploadResult.fileSize())
                    .userId(userId)
                    .securityStatus("safe")
                    .build();

        } catch (Exception e) {
            log.error("Error processing file: {}", fileName, e);
            throw new RuntimeException("Failed to process file", e);
        }
    }

    private String detectFileType(InputStream stream, String fileName) throws Exception {
        org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
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

    private byte[] readInputStreamToBytes(InputStream inputStream) throws java.io.IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}