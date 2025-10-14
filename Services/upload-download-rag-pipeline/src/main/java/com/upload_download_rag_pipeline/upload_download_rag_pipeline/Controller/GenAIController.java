package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Controller;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model.ProcessedDocument;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
@RestController
@RequestMapping("/api/genai")
@RequiredArgsConstructor
public class GenAIController {

    private final UploadService uploadService;

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<ProcessedDocument>> processFile(
            @RequestPart("file") FilePart file,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        final String userId = resolveUserId(jwt);

        log.info("Receiving file for processing (reactive): filename={} userId={}", file.filename(), userId);

        // Create a secure temp file to stream to
        Mono<Path> tempFileMono = Mono.fromCallable(() -> {
            Path tmp = Files.createTempFile("upload-", "-" + sanitize(file.filename()));
            // Pre-create empty file to ensure existence
            Files.newByteChannel(tmp, StandardOpenOption.WRITE).close();
            return tmp;
        }).subscribeOn(Schedulers.boundedElastic());

        return tempFileMono.flatMap(tmpPath ->
                // Stream the incoming file to the temp file (reactive write)
                DataBufferUtils.write(file.content(), tmpPath, StandardOpenOption.WRITE)
                        .then(Mono.defer(() ->
                                // Now call your existing blocking service off the event loop
                                Mono.fromCallable(() -> {
                                    try (InputStream in = Files.newInputStream(tmpPath, StandardOpenOption.READ)) {
                                        return uploadService.processFile(
                                                in,
                                                file.filename(),
                                                userId,
                                                jwt
                                        );
                                    }
                                }).subscribeOn(Schedulers.boundedElastic())
                        ))
                        // Map to appropriate HTTP status
                        .map(result -> {
                            if ("unsafe".equalsIgnoreCase(result.getSecurityStatus())) {
                                return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
                            }
                            return ResponseEntity.ok(result);
                        })
                        // Always clean up temp file
                        .doFinally(sig -> {
                            try {
                                Files.deleteIfExists(tmpPath);
                            } catch (Exception ex) {
                                log.warn("Failed deleting temp file {}: {}", tmpPath, ex.getMessage());
                            }
                        })
        ).onErrorResume(ex -> {
            log.error("Error processing reactive file upload", ex);
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        });
    }

    private String resolveUserId(Jwt jwt) {
        if (jwt == null) return "anonymous";
        // Try common claim names; fall back to subject
        Object userId = jwt.getClaims().getOrDefault("userId",
                jwt.getClaims().getOrDefault("uid",
                        jwt.getClaims().getOrDefault("sub", "anonymous")));
        return String.valueOf(userId);
    }

    private static String sanitize(String name) {
        if (name == null) return "file";
        // Keep it simple: remove path separators & control chars
        return name.replaceAll("[\\\\/\\r\\n\\t]", "_");
    }
}
