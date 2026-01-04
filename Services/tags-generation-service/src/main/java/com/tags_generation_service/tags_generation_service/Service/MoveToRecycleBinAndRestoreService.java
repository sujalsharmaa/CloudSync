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
    @Transactional
    public Boolean moveToRecycleBin(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return false;
        }

        List<FileMetadataPostgres> existingFiles = new ArrayList<>();
        fileMetadataPostgresRepository.findAllById(fileIds).forEach(existingFiles::add);

        if (existingFiles.isEmpty()) {
            return false;
        }

        for (FileMetadataPostgres file : existingFiles) {
            file.setIsMovedToRecycleBin(true);
            queueService.publishFileRequest(file);
        }

        fileMetadataPostgresRepository.saveAll(existingFiles);

        return true;
    }

    @Transactional
    public Boolean RestoreFiles(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return false;
        }

        List<FileMetadataPostgres> existingFiles = new ArrayList<>();
        fileMetadataPostgresRepository.findAllById(fileIds).forEach(existingFiles::add);

        if (existingFiles.isEmpty()) {
            return false;
        }

        for (FileMetadataPostgres file : existingFiles) {
            file.setIsMovedToRecycleBin(false);
            queueService.publishFileRequest(file);
        }

        fileMetadataPostgresRepository.saveAll(existingFiles);

        return true;
    }
}
