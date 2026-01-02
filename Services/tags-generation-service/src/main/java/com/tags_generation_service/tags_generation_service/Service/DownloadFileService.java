package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Exception.BusinessException; // Import
import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadFileService {

    private final S3Service s3Service;
    private final FileMetadataPostgresRepository fileMetadataPostgresRepository;

    public byte[] downloadAndZipFiles(List<UUID> fileIds) throws IOException {
        List<FileMetadataPostgres> filesToDownload = fileMetadataPostgresRepository.findAllById(fileIds);

        if (filesToDownload.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            for (FileMetadataPostgres file : filesToDownload) {
                try (InputStream inputStream = s3Service.downloadFile(file.getS3Location())) {
                    ZipEntry zipEntry = new ZipEntry(file.getFileName());
                    zipOut.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, len);
                    }
                    zipOut.closeEntry();
                } catch (IOException e) {
                    log.error("Failed to download or zip file {}: {}", file.getFileName(), e.getMessage());
                    // We can choose to skip this file (partial success) or fail the whole request.
                    // For now, logging and continuing is a valid strategy for bulk download,
                    // but if strict consistency is needed:
                    // throw new BusinessException("Failed to download file: " + file.getFileName());
                }
            }
            zipOut.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Error processing zip stream: " + e.getMessage());
        }
    }
}