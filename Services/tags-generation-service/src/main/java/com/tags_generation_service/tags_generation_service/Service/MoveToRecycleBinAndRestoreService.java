package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MoveToRecycleBinAndRestoreService {

    private final FileMetadataPostgresRepository fileMetadataPostgresRepository;
    private final QueueService queueService;

    /**
     * Move given files to recycle bin.
     * @param fileIds list of file UUIDs
     * @return number of files successfully moved
     */
    @Transactional
    public Boolean moveToRecycleBin(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return false;
        }

        // Fetch all existing files for the given IDs
        List<FileMetadataPostgres> existingFiles = new ArrayList<>();
        fileMetadataPostgresRepository.findAllById(fileIds).forEach(existingFiles::add);

        if (existingFiles.isEmpty()) {
            return false;
        }

        // Mark each as moved and publish to queue
        for (FileMetadataPostgres file : existingFiles) {
            file.setIsMovedToRecycleBin(true);
            // publish updated file to ES update queue (do this before/after save based on your design)
            queueService.publishFileRequest(file);
        }

        // Persist changes in batch
        fileMetadataPostgresRepository.saveAll(existingFiles);

        return true;
    }

    @Transactional
    public Boolean RestoreFiles(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return false;
        }

        // Fetch all existing files for the given IDs
        List<FileMetadataPostgres> existingFiles = new ArrayList<>();
        fileMetadataPostgresRepository.findAllById(fileIds).forEach(existingFiles::add);

        if (existingFiles.isEmpty()) {
            return false;
        }

        // Mark each as moved and publish to queue
        for (FileMetadataPostgres file : existingFiles) {
            file.setIsMovedToRecycleBin(false);
            // publish updated file to ES update queue (do this before/after save based on your design)
            queueService.publishFileRequest(file);
        }

        // Persist changes in batch
        fileMetadataPostgresRepository.saveAll(existingFiles);

        return true;
    }
}
