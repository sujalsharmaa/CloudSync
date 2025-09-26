package com.tags_generation_service.tags_generation_service.Controllers;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class FileControllers {
    private final MoveToRecycleBinAndRestoreService MoveToRecycleBinAndRestoreService;
    private final DeletePermanently deletePermanently;
    private final DownloadFileService downloadFileService;
    private final StarService starService;

    @DeleteMapping("/MoveToRecycleBin")
    public ResponseEntity<Boolean> MoveToRecycleBin(@RequestBody List<UUID> FileId, @AuthenticationPrincipal Jwt jwt){
        System.out.println(FileId);
        Boolean response = MoveToRecycleBinAndRestoreService.moveToRecycleBin(FileId);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/RestoreFiles")
    public ResponseEntity<Boolean> RestoreFiles(@RequestBody List<UUID> FileId, @AuthenticationPrincipal Jwt jwt){
        System.out.println(FileId);
        Boolean response = MoveToRecycleBinAndRestoreService.RestoreFiles(FileId);
        return ResponseEntity.ok(response);
    }
    // A POST request is appropriate here because we are changing the state of a resource.
    @PostMapping("/star/{fileId}")
    public ResponseEntity<Boolean> updateStarStatus(@PathVariable UUID fileId, @RequestBody Boolean isStarred, @AuthenticationPrincipal Jwt jwt) {
        Boolean success = starService.UpdateStar(fileId, isStarred);
        if (success) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
    }

    @PostMapping("/DownloadFiles")
    public ResponseEntity<byte[]> downloadFiles(@RequestBody List<UUID> fileIds, @AuthenticationPrincipal Jwt jwt) {
        if (fileIds == null || fileIds.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            byte[] zipBytes = downloadFileService.downloadAndZipFiles(fileIds);

            if (zipBytes.length == 0) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "files.zip");
            headers.setContentLength(zipBytes.length);

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error creating zip file for download", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/PermanentlyDeleteFiles")
    public ResponseEntity<Boolean> PermanentlyDeleteFiles(@RequestBody List<UUID> FileId, @AuthenticationPrincipal Jwt jwt){
        System.out.println(FileId);
        Boolean response = deletePermanently.DeleteFilePermanently(FileId);
        return ResponseEntity.ok(response);
    }
}