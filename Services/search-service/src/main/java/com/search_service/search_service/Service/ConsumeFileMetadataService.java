package com.search_service.search_service.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search_service.search_service.Model.FileMetadata;
import com.search_service.search_service.Repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumeFileMetadataService {

    // Define a key prefix for the cache you want to invalidate.
    private static final String USER_FILE_CACHE_PREFIX = "search:files:user:";
    // Key format from RedisFileService: 'pending_files:USER_ID'
    private static final String PENDING_FILE_KEY_PREFIX = "pending_files:";

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final FileMetadataRepository fileMetadataRepository;

    /**
     * This method listens for new messages on the Kafka topic and processes them.
     */
    @KafkaListener(topics = "file-metadata-search", groupId = "rag-pipeline-group")
    public void listen(String message) {
        log.info("Received message from Kafka: {}", message);

        try {
            // 1. Deserialize the JSON message
            FileMetadata fileMetadata = objectMapper.readValue(message, FileMetadata.class);

            // Set the processedAt date
            if (fileMetadata.getProcessedAt() == null) {
                fileMetadata.setProcessedAt(new Date());
            }

            // 2. Save the FileMetadata object to Elasticsearch (the source of truth)
            fileMetadataRepository.save(fileMetadata);
            log.info("Successfully saved metadata for file: {}", fileMetadata.getFileName(), fileMetadata.getFileSize());

            String userId = fileMetadata.getUserId();
            String s3Key = userId + "/" + fileMetadata.getFileName(); // Reconstruct S3 key based on standard pattern

            // 3. INVALIDATE THE USER'S CACHE
            String cacheKeyToInvalidate = USER_FILE_CACHE_PREFIX + userId;
            Boolean deleted = redisTemplate.delete(cacheKeyToInvalidate);
            log.info("Search cache invalidated for user {}. Key: {} | Deleted: {}", userId, cacheKeyToInvalidate, deleted);

            // 4. CLEAN UP THE PENDING FILE LIST IN REDIS
            removePendingFile(userId, s3Key);

            log.info("Cache invalidation process complete for file: {}", fileMetadata.getFileName());

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
        }
    }

    /**
     * Removes the S3 key from the user's pending file list in Redis.
     * This ensures the eventual consistency marker is cleaned up after indexing.
     */
    private void removePendingFile(String userId, String s3Key) {
        String redisKey = PENDING_FILE_KEY_PREFIX + userId;

        try {
            String existingJson = redisTemplate.opsForValue().get(redisKey);

            if (existingJson == null) {
                log.warn("Pending file cleanup requested for user {}, but no pending list was found in Redis.", userId);
                return;
            }

            // Deserialize the existing JSON list
            List<String> pendingS3Keys = objectMapper.readValue(existingJson, new TypeReference<List<String>>() {});

            // Remove the processed S3 key
            boolean wasRemoved = pendingS3Keys.remove(s3Key);

            if (wasRemoved) {
                if (pendingS3Keys.isEmpty()) {
                    // If the list is empty, delete the key entirely
                    redisTemplate.delete(redisKey);
                    log.info("Removed final pending file for user {}. Deleted pending list key: {}", userId, redisKey);
                } else {
                    // Serialize and save the updated list
                    String updatedJson = objectMapper.writeValueAsString(pendingS3Keys);
                    // NOTE: We don't reapply TTL here, as the TTL was originally set by the uploading service.
                    redisTemplate.opsForValue().set(redisKey, updatedJson);
                    log.info("Removed pending file {} for user {}. Remaining pending: {}",
                            s3Key, userId, pendingS3Keys.size());
                }
            } else {
                log.warn("Pending file {} for user {} was not found in the Redis list during cleanup.", s3Key, userId);
            }

        } catch (IOException e) {
            log.error("Error performing pending file cleanup for Redis user {}.", userId, e);
        }
    }
}
