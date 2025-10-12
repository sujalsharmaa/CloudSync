package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Controller;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.ProcessedDocument;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/genai")
@RequiredArgsConstructor
public class  GenAIController {

    private final UploadService uploadService;

    @PostMapping("/process")
    public ResponseEntity<ProcessedDocument> processFile(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaims().get("userId").toString();
        System.out.println("userId"+userId);

        try {
            log.info("Receiving file for processing: {}", file.getOriginalFilename());
            log.info("Current user id (principal): {}", userId);
            System.out.println(userId);
            ProcessedDocument result = uploadService.processFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    userId,
                    jwt
            );

            if ("unsafe".equalsIgnoreCase(result.getSecurityStatus())) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}