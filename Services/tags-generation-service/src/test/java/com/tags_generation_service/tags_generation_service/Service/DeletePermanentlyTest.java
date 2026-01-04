package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePermanentlyTest {

    @Mock
    private FileMetadataPostgresRepository repository;

    @Mock
    private QueueService queueService;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private DeletePermanently deletePermanently;

    private List<UUID> fileIds;
    private List<FileMetadataPostgres> files;

    @BeforeEach
    void setUp() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        fileIds = Arrays.asList(id1, id2);

        FileMetadataPostgres file1 = new FileMetadataPostgres();
        file1.setId(id1);
        file1.setS3Location("https://bucket.s3.region.amazonaws.com/user123/file1.pdf");
        file1.setThumbnailS3Location("https://bucket.s3.region.amazonaws.com/user123/thumb1.jpg");

        FileMetadataPostgres file2 = new FileMetadataPostgres();
        file2.setId(id2);
        file2.setS3Location("https://bucket.s3.region.amazonaws.com/user123/file2.pdf");

        files = Arrays.asList(file1, file2);
    }

    @Test
    void deleteFilePermanently_ValidFiles_ShouldDeleteSuccessfully() {
        // Arrange
        when(repository.findAllById(fileIds)).thenReturn(files);

        Boolean result = deletePermanently.DeleteFilePermanently(fileIds);


        assertTrue(result);

        verify(s3Service, times(2)).deleteFiles(anyList());

        verify(repository, times(1)).deleteAllById(fileIds);
        verify(queueService, times(2)).deleteFileRequest(anyString());
    }

    @Test
    void deleteFilePermanently_EmptyList_ShouldReturnFalse() {
        // Act
        Boolean result = deletePermanently.DeleteFilePermanently(Arrays.asList());

        // Assert
        assertFalse(result);
        verify(repository, never()).findAllById(any());
        verify(s3Service, never()).deleteFiles(any());
    }

    @Test
    void deleteFilePermanently_NoFilesFound_ShouldReturnFalse() {
        // Arrange
        when(repository.findAllById(fileIds)).thenReturn(Arrays.asList());

        // Act
        Boolean result = deletePermanently.DeleteFilePermanently(fileIds);

        // Assert
        assertFalse(result);
        verify(s3Service, never()).deleteFiles(any());
        verify(repository, never()).deleteAllById(any());
    }
}