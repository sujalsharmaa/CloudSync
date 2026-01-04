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

    @KafkaListener(topics = "file-metadata-delete", groupId = "rag-pipeline-group")
    public void listen(String message) {
        log.info("Received message from Kafka: {}", message);
        try {
            String fileId = objectMapper.readValue(message, String.class);
            fileMetadataRepository.deleteById(fileId);
            log.info("Successfully deleted metadata for file: {}", fileId);

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);

        }
    }
}