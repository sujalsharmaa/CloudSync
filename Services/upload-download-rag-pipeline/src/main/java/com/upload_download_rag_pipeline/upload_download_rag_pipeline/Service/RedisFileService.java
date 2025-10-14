package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisFileService {

    // Key format: 'pending_files:USER_ID'
    private static final String PENDING_FILE_KEY_PREFIX = "pending_files:";
    // TTL for the list (e.g., 1 hour, allowing pipeline enough time to process)
    private static final Duration PENDING_FILE_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper; // Inject ObjectMapper for JSON serialization/deserialization

    /**
     * Saves a list of S3 keys for a user in Redis, indicating files are pending RAG processing.
     * The key is user-specific (userId) and the value is a JSON list of S3 keys.
     *
     * @param userId The ID of the user.
     * @param s3Key The S3 key (path) of the newly uploaded file.
     */
    public void savePendingFileMarker(String userId, String s3Key) {
        String redisKey = PENDING_FILE_KEY_PREFIX + userId;

        try {
            // 1. Retrieve existing list (read-through)
            String existingJson = redisTemplate.opsForValue().get(redisKey);
            List<String> pendingS3Keys;

            if (existingJson != null) {
                // Deserialize the existing JSON list
                pendingS3Keys = objectMapper.readValue(existingJson, new TypeReference<List<String>>() {});
            } else {
                pendingS3Keys = new ArrayList<>();
            }

            // 2. Add the new S3 key
            if (!pendingS3Keys.contains(s3Key)) {
                pendingS3Keys.add(s3Key);
            }

            // 3. Serialize and save the updated list (write-back)
            String updatedJson = objectMapper.writeValueAsString(pendingS3Keys);

            // Set the value with TTL. This overwrites the existing list.
            redisTemplate.opsForValue().set(redisKey, updatedJson, PENDING_FILE_TTL);
            log.info("Saved Redis pending file list for user {}. Added file: {} (Total pending: {})",
                    userId, s3Key, pendingS3Keys.size());

        } catch (IOException e) {
            log.error("Error serializing or deserializing file list for Redis user {}.", userId, e);
        }
    }
}
