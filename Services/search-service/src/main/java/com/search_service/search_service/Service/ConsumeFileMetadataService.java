package com.search_service.search_service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.search_service.search_service.Dto.SavedFileDto;
import com.search_service.search_service.Model.FileMetadata;
import com.search_service.search_service.Repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate; // <-- Now correctly used
import java.util.Date;
import java.util.Optional;
import java.time.Duration; // <-- New import

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumeFileMetadataService {

    // Key prefix for the UploadService to wait on (Must match UploadService)
    private static final String CONFIRMATION_KEY_PREFIX = "file:sync_confirm:";
    private static final String PENDING_FILE_KEY_PREFIX = "pending_files:";

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate; // Redis used for synchronization
    private final FileMetadataRepository fileMetadataRepository;

    /**
     * This method listens for new messages on the Kafka topic, processes them,
     * and sends a completion signal back via Redis.
     */
    @KafkaListener(topics = "file-metadata-search", groupId = "rag-pipeline-group")
    public void listen(String message) {
        log.info("Received message from Kafka: {}", message);

        String userId = null;
        String fileName = null;

        try {
            // 1. Deserialize the JSON message
            FileMetadata fileMetadata = objectMapper.readValue(message, FileMetadata.class);
            userId = fileMetadata.getUserId();
            fileName = fileMetadata.getFileName();

            // Set the processedAt date
            if (fileMetadata.getProcessedAt() == null) {
                fileMetadata.setProcessedAt(new Date());
            }

            // 2. Save the FileMetadata object to Elasticsearch
            fileMetadataRepository.save(fileMetadata);
            log.info("Successfully saved metadata for file: {}", fileName, fileMetadata.getFileSize());

            // 3. --- Send Confirmation Signal to UploadService via Redis ---
            // The unique key is based on the user ID and file name
            String confirmationKey = CONFIRMATION_KEY_PREFIX + userId + ":" + fileName;

            // Set the key to signal completion.
            redisTemplate.opsForValue().set(confirmationKey, "COMPLETE", Duration.ofSeconds(30));
            log.info("Set Redis confirmation key: {}", confirmationKey);
            // -----------------------------------------------------------

            // 4. (Optional) Invalidate Caches for eventual consistency
            // Invalidate the main search cache for the user's file list
            redisTemplate.delete("search:files:user:" + userId);

            // Remove the file from the pending file list (if implemented)
            // Note: Since the file name is used to identify the pending file,
            // we remove the element from the list stored under the user ID.
            String pendingKey = PENDING_FILE_KEY_PREFIX + userId;

            // Assuming the list contains the file name string
            redisTemplate.opsForList().remove(pendingKey, 1, fileName);

            log.info("Cache cleanup initiated and pending file marker removed for user {}", userId);

        } catch (Exception e) {
            log.error("Error processing Kafka message for file {}: {}", fileName, e);
        }
    }
}
