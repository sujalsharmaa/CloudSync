
package com.tags_generation_service.tags_generation_service.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String metadataTopic = "file-metadata-search";
    private final String FileDeleteTopic = "file-metadata-delete";

    public void publishFileRequest(FileMetadataPostgres fileMetadataPostgres) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(fileMetadataPostgres);
            log.info("Published message to Kafka topic '{}' for file: {}", metadataTopic, fileMetadataPostgres.getFileName());
            kafkaTemplate.send(metadataTopic, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize FileMetadataPostgres to JSON", e);
        }
    }
    public void deleteFileRequest(String FileId) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(FileId);
            log.info("Published message to Kafka topic '{}' for file: {}", FileDeleteTopic, FileId);
            kafkaTemplate.send(FileDeleteTopic, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize FileMetadataPostgres to JSON", e);
        }
    }
}