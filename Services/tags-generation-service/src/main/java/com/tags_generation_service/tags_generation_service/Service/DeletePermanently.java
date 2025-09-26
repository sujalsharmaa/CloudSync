// DeletePermanently.java
package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeletePermanently {

    private final FileMetadataPostgresRepository fileMetadataPostgresRepository;
    private final QueueService queueService;
    private final S3Service s3Service; // Inject S3Service

    @Transactional
    public Boolean DeleteFilePermanently(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return false;
        }

        // 1. Get file metadata to find the S3 object keys
        List<FileMetadataPostgres> filesToDelete = fileMetadataPostgresRepository.findAllById(fileIds);
        List<String> s3Keys = filesToDelete.stream()
                .map(FileMetadataPostgres::getFileName) // Assuming fileName is the S3 key
                .collect(Collectors.toList());

        // 2. Delete the files from S3
        if (!s3Keys.isEmpty()) {
            s3Service.deleteFiles(s3Keys);
        }

        // 3. Delete the metadata from the PostgreSQL database
        fileMetadataPostgresRepository.deleteAllById(fileIds);

        // 4. Publish deletion requests to the Kafka queue for Elasticsearch
        for (UUID fileId : fileIds) {
            queueService.deleteFileRequest(String.valueOf(fileId));
        }

        return true;
    }
}