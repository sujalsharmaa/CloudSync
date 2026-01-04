package com.tags_generation_service.tags_generation_service.Controllers;

import com.tags_generation_service.tags_generation_service.Exception.BusinessException; // Import
import com.tags_generation_service.tags_generation_service.Exception.ResourceNotFoundException; // Import
import com.tags_generation_service.tags_generation_service.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class FileControllers {
    private final MoveToRecycleBinAndRestoreService moveToRecycleBinAndRestoreService;
    private final DeletePermanently deletePermanently;
    private final DownloadFileService downloadFileService;
    private final StarService starService;

    @DeleteMapping("/MoveToRecycleBin")
    public ResponseEntity<Boolean> moveToRecycleBin(@RequestBody List<UUID> fileIds, @AuthenticationPrincipal Jwt jwt) {
        Boolean response = moveToRecycleBinAndRestoreService.moveToRecycleBin(fileIds);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/RestoreFiles")
    public ResponseEntity<Boolean> restoreFiles(@RequestBody List<UUID> fileIds, @AuthenticationPrincipal Jwt jwt) {
        Boolean response = moveToRecycleBinAndRestoreService.RestoreFiles(fileIds);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/star/{fileId}")
    public ResponseEntity<Boolean> updateStarStatus(@PathVariable UUID fileId, @RequestBody Boolean isStarred, @AuthenticationPrincipal Jwt jwt) {
        Boolean success = starService.UpdateStar(fileId, isStarred);
        if (!success) {
            throw new ResourceNotFoundException("File", "id", fileId);
        }
        return ResponseEntity.ok(true);
    }

    @PostMapping("/DownloadFiles")
    public ResponseEntity<byte[]> downloadFiles(@RequestBody List<UUID> fileIds, @AuthenticationPrincipal Jwt jwt) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException("No files selected for download.");
        }

        try {
            byte[] zipBytes = downloadFileService.downloadAndZipFiles(fileIds);

            if (zipBytes.length == 0) {
                throw new BusinessException("Selected files could not be found or downloaded.");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "files.zip");
            headers.setContentLength(zipBytes.length);

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error creating zip file for download", e);
            throw new BusinessException("Failed to generate download zip file: " + e.getMessage());
        }
    }

    @DeleteMapping("/PermanentlyDeleteFiles")
    public ResponseEntity<Boolean> permanentlyDeleteFiles(@RequestBody List<UUID> fileIds, @AuthenticationPrincipal Jwt jwt) {
        Boolean response = deletePermanently.DeleteFilePermanently(fileIds);
        return ResponseEntity.ok(response);
    }
}