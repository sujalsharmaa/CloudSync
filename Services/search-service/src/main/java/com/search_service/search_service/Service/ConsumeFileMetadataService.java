package com.search_service.search_service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.search_service.search_service.Model.FileMetadata;
import com.search_service.search_service.Repository.FileMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
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


    @KafkaListener(topics = "file-metadata-search", groupId = "rag-pipeline-group")
    public void listen(String message) throws Exception {
            FileMetadata fileMetadata = objectMapper.readValue(message, FileMetadata.class);
            String fileName = fileMetadata.getFileName();
            String userId = fileMetadata.getUserId();

            log.info("Received metadata for file: {}", fileName);

            if (fileMetadata.getProcessedAt() == null) {
                fileMetadata.setProcessedAt(new Date());
            }

            fileMetadataRepository.save(fileMetadata);
            String fileId = fileMetadata.getId();
            log.info("Successfully saved metadata for file: {} with ID: {}", fileName, fileId);

            if (userId != null && !userId.isEmpty()) {
                String confirmationKey = CONFIRMATION_KEY_PREFIX + userId + ":" + fileName;

                redisTemplate.opsForValue().set(confirmationKey, fileId, Duration.ofSeconds(120));
                log.info("Set Redis confirmation key: {} with value (fileId): {}", confirmationKey, fileId);
            } else {
                log.warn("Cannot set confirmation for file {} as userId is missing.", fileName);
            }

            if (userId != null && !userId.isEmpty()) {
                log.info("Invalidating file cache for user: {}", userId);
                searchService.evictUserFileCache(userId);
            } else {
                log.warn("Cannot invalidate cache for file {} as userId is missing.", fileName);
            }

    }
}