package com.tags_generation_service.tags_generation_service.Service;// Services/tags-generation-service/src/test/java/com/tags_generation_service/tags_generation_service/Service/MoveToRecycleBinAndRestoreServiceTest.java
import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveToRecycleBinAndRestoreServiceTest {

    @Mock
    private FileMetadataPostgresRepository repository;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private MoveToRecycleBinAndRestoreService service;

    @Captor
    private ArgumentCaptor<List<FileMetadataPostgres>> filesCaptor;

    private List<UUID> fileIds;
    private List<FileMetadataPostgres> files;

    @BeforeEach
    void setUp() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        fileIds = Arrays.asList(id1, id2);

        FileMetadataPostgres file1 = new FileMetadataPostgres();
        file1.setId(id1);
        file1.setIsMovedToRecycleBin(false);

        FileMetadataPostgres file2 = new FileMetadataPostgres();
        file2.setId(id2);
        file2.setIsMovedToRecycleBin(false);

        files = Arrays.asList(file1, file2);
    }

    @Test
    void moveToRecycleBin_ValidFiles_ShouldMoveSuccessfully() {
        // Arrange
        when(repository.findAllById(fileIds)).thenReturn(files);
        when(repository.saveAll(any())).thenReturn(files);

        // Act
        Boolean result = service.moveToRecycleBin(fileIds);

        // Assert
        assertTrue(result);
        verify(repository, times(1)).saveAll(filesCaptor.capture());
        List<FileMetadataPostgres> savedFiles = filesCaptor.getValue();
        assertTrue(savedFiles.stream().allMatch(FileMetadataPostgres::getIsMovedToRecycleBin));
        verify(queueService, times(2)).publishFileRequest(any());
    }

    @Test
    void moveToRecycleBin_EmptyList_ShouldReturnFalse() {
        // Act
        Boolean result = service.moveToRecycleBin(Arrays.asList());

        // Assert
        assertFalse(result);
        verify(repository, never()).saveAll(any());
    }

    @Test
    void restoreFiles_ValidFiles_ShouldRestoreSuccessfully() {
        // Arrange
        files.forEach(f -> f.setIsMovedToRecycleBin(true));
        when(repository.findAllById(fileIds)).thenReturn(files);
        when(repository.saveAll(any())).thenReturn(files);

        // Act
        Boolean result = service.RestoreFiles(fileIds);

        // Assert
        assertTrue(result);
        verify(repository, times(1)).saveAll(filesCaptor.capture());
        List<FileMetadataPostgres> savedFiles = filesCaptor.getValue();
        assertTrue(savedFiles.stream().noneMatch(FileMetadataPostgres::getIsMovedToRecycleBin));
        verify(queueService, times(2)).publishFileRequest(any());
    }
}