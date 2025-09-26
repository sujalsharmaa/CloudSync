package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class StarService {
    private final FileMetadataPostgresRepository fileMetadataPostgresRepository;
    private final QueueService queueService;
    public Boolean UpdateStar(UUID fileId, Boolean isStarredStatus) {
        // Use a single query to find the file and avoid a race condition.
        Optional<FileMetadataPostgres> fileOptional = fileMetadataPostgresRepository.findById(fileId);

        if (fileOptional.isPresent()) {
            FileMetadataPostgres file = fileOptional.get();
            file.setIsStarred(isStarredStatus);
            // Crucial step: save the updated file back to the database.
            fileMetadataPostgresRepository.save(file);
            queueService.publishFileRequest(file);
            log.info("Successfully updated star status for file: {}", fileId);
            return true;
        } else {
            log.warn("File does not exist with ID: {}", fileId);
            return false;
        }
    }
}