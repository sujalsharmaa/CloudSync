package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Exception.ResourceNotFoundException; // Import
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
        FileMetadataPostgres file = fileMetadataPostgresRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        file.setIsStarred(isStarredStatus);
        fileMetadataPostgresRepository.save(file);

        // Asynchronous operation
        queueService.publishFileRequest(file);

        log.info("Successfully updated star status for file: {}", fileId);
        return true;
    }
}