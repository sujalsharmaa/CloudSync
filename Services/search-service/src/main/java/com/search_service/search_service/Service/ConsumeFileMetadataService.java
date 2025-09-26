package com.search_service.search_service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.search_service.search_service.Model.FileMetadata;
import com.search_service.search_service.Repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumeFileMetadataService {

    private final ObjectMapper objectMapper;
    private final FileMetadataRepository fileMetadataRepository;

    /**
     * This method listens for new messages on the Kafka topic and processes them.
     */
    @KafkaListener(topics = "file-metadata-search", groupId = "rag-pipeline-group")
    public void listen(String message) {
        log.info("Received message from Kafka: {}", message);
        try {
            // Deserialize the JSON message directly into the FileMetadata model
            FileMetadata fileMetadata = objectMapper.readValue(message, FileMetadata.class);

            // Set the processedAt date as it may not be in the incoming JSON
            if (fileMetadata.getProcessedAt() == null) {
                fileMetadata.setProcessedAt(new Date());
            }

            // Save the FileMetadata object to Elasticsearch
            fileMetadataRepository.save(fileMetadata);
            log.info("Successfully saved metadata for file: {}", fileMetadata.getFileName(),fileMetadata.getFileSize());
            Optional<FileMetadata> fileMetadata1 = fileMetadataRepository.findById(fileMetadata.getId());
            log.info("saved file metadata "+ fileMetadata1.get().getFileSize());

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
            // In a real-world scenario, you would handle this gracefully, e.g.,
            // by pushing it to a dead-letter topic.
        }
    }
}