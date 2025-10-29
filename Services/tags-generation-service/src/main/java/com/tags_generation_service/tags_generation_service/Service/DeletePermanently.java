// DeletePermanently.java
package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletePermanently {

    private final FileMetadataPostgresRepository fileMetadataPostgresRepository;
    private final QueueService queueService;
    private final S3Service s3Service;

    @Transactional
    public Boolean DeleteFilePermanently(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            log.warn("DeleteFilePermanently called with null or empty fileIds");
            return false;
        }

        // 1. Get file metadata to find the S3 object keys
        List<FileMetadataPostgres> filesToDelete = fileMetadataPostgresRepository.findAllById(fileIds);

        if (filesToDelete.isEmpty()) {
            log.warn("No files found for the provided IDs: {}", fileIds);
            return false;
        }

        // 2. Extract S3 keys from s3Location and convert to actual keys
        List<String> s3Keys = filesToDelete.stream()
                .map(file -> extractS3KeyFromLocation(file.getS3Location()))
                .filter(key -> key != null && !key.isEmpty())
                .collect(Collectors.toList());

        // 3. Delete the files from S3
        if (!s3Keys.isEmpty()) {
            log.info("Deleting {} files from S3", s3Keys.size());
            s3Service.deleteFiles(s3Keys);

            // Also delete thumbnails if they exist
            List<String> thumbnailKeys = filesToDelete.stream()
                    .map(FileMetadataPostgres::getThumbnailS3Location)
                    .filter(location -> location != null && !location.isEmpty())
                    .map(this::extractS3KeyFromLocation)
                    .filter(key -> key != null && !key.isEmpty())
                    .collect(Collectors.toList());

            if (!thumbnailKeys.isEmpty()) {
                log.info("Deleting {} thumbnail files from S3", thumbnailKeys.size());
                s3Service.deleteFiles(thumbnailKeys);
            }
        } else {
            log.warn("No valid S3 keys found for deletion");
        }

        // 4. Delete the metadata from the PostgreSQL database
        fileMetadataPostgresRepository.deleteAllById(fileIds);
        log.info("Deleted {} files from PostgreSQL", filesToDelete.size());

        // 5. Publish deletion requests to the Kafka queue for Elasticsearch
        for (UUID fileId : fileIds) {
            queueService.deleteFileRequest(String.valueOf(fileId));
        }
        log.info("Published {} deletion requests to Kafka", fileIds.size());

        return true;
    }

    /**
     * Extract the S3 key from the full S3 location URL.
     * Example: "https://bucket.s3.region.amazonaws.com/userId/file.pdf" -> "userId/file.pdf"
     */
    private String extractS3KeyFromLocation(String s3Location) {
        if (s3Location == null || s3Location.isEmpty()) {
            return null;
        }

        // If it's already just a key (doesn't start with http), return as is
        if (!s3Location.startsWith("http")) {
            return s3Location;
        }

        // Extract key from URL format
        try {
            // Example: https://bucket.s3.region.amazonaws.com/userId/file.pdf
            // We want: userId/file.pdf
            int lastSlashIndex = s3Location.indexOf(".com/");
            if (lastSlashIndex != -1) {
                return s3Location.substring(lastSlashIndex + 5); // +5 to skip ".com/"
            }

            // Fallback: just take everything after the third slash
            String[] parts = s3Location.split("/", 4);
            if (parts.length >= 4) {
                return parts[3];
            }
        } catch (Exception e) {
            log.error("Error extracting S3 key from location: {}", s3Location, e);
        }

        return null;
    }
}