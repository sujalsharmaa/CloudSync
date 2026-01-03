package com.search_service.search_service.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search_service.search_service.Model.FileMetadata;
import com.search_service.search_service.Repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
public class ConsumeFileMetadataService {

    private final ObjectMapper objectMapper;
    private final FileMetadataRepository fileMetadataRepository;
    private final StringRedisTemplate redisTemplate;
    private final SearchService searchService;
    private static final String CONFIRMATION_KEY_PREFIX = "file:sync_confirm:";

    @Autowired
    public ConsumeFileMetadataService(ObjectMapper objectMapper,
                                      FileMetadataRepository fileMetadataRepository,
                                      StringRedisTemplate redisTemplate,
                                      SearchService searchService) {
        this.objectMapper = objectMapper;
        this.fileMetadataRepository = fileMetadataRepository;
        this.redisTemplate = redisTemplate;
        this.searchService = searchService;
    }

    /**
     * Listens to the Kafka topic for new file metadata and saves it to Elasticsearch.
     * Also signals completion via Redis by setting a key that the upload service is polling.
     *
     * @param message The JSON string message containing file metadata.
     */
    @KafkaListener(topics = "file-metadata-search", groupId = "rag-pipeline-group")
    public void listen(String message) throws Exception {
            // 1. Deserialize the message into FileMetadata object
            FileMetadata fileMetadata = objectMapper.readValue(message, FileMetadata.class);
            String fileName = fileMetadata.getFileName();
            String userId = fileMetadata.getUserId();

            log.info("Received metadata for file: {}", fileName);

            // 2. Ensure processedAt is set if not already present
            if (fileMetadata.getProcessedAt() == null) {
                fileMetadata.setProcessedAt(new Date());
            }

            // 3. Save the FileMetadata object to Elasticsearch
            fileMetadataRepository.save(fileMetadata);
            String fileId = fileMetadata.getId();
            log.info("Successfully saved metadata for file: {} with ID: {}", fileName, fileId);

            // --- 4. SIGNAL CONFIRMATION VIA REDIS ---
            if (userId != null && !userId.isEmpty()) {
                String confirmationKey = CONFIRMATION_KEY_PREFIX + userId + ":" + fileName;

                // Set the fileId with a TTL of 120 seconds
                // This gives the upload service plenty of time to retrieve it
                redisTemplate.opsForValue().set(confirmationKey, fileId, Duration.ofSeconds(120));
                log.info("Set Redis confirmation key: {} with value (fileId): {}", confirmationKey, fileId);
            } else {
                log.warn("Cannot set confirmation for file {} as userId is missing.", fileName);
            }
            // ---------------------------------------------------

            // --- 5. INVALIDATE USER'S FILE CACHE ---
            if (userId != null && !userId.isEmpty()) {
                log.info("Invalidating file cache for user: {}", userId);
                searchService.evictUserFileCache(userId);
            } else {
                log.warn("Cannot invalidate cache for file {} as userId is missing.", fileName);
            }

    }
}