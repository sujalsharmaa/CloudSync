package com.search_service.search_service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.search_service.search_service.Repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteFileMetadataService {

    private final ObjectMapper objectMapper;
    private final FileMetadataRepository fileMetadataRepository;

    /**
     * This method listens for new messages on the Kafka topic and deletes them.
     */
    @KafkaListener(topics = "file-metadata-delete", groupId = "rag-pipeline-group")
    public void listen(String message) {
        log.info("Received message from Kafka: {}", message);
        try {
            // The message is a JSON string of a UUID, so deserialize it directly to String
            // The message format from the producer (QueueService) is a single string of the UUID.
            String fileId = objectMapper.readValue(message, String.class);

            // Delete the FileMetadata object from Elasticsearch
            fileMetadataRepository.deleteById(fileId);
            log.info("Successfully deleted metadata for file: {}", fileId);

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
            // In a real-world scenario, you would handle this gracefully, e.g.,
            // by pushing it to a dead-letter topic.
        }
    }
}