package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.BanNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    // Ensure your Kafka configuration is set up to handle String keys and String values
    private final KafkaTemplate<String, String> kafkaTemplate;
    // Use ObjectMapper to serialize Java objects to JSON strings
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String metadataTopic = "file-metadata-requests";
    private final String notificationTopic = "notification-topic";

    /**
     * Publishes a message to a Kafka topic to trigger asynchronous metadata processing.
     * The message contains a JSON string with the file details.
     */
    public void publishMetadataRequest(String fileName, String fileType, String s3Location, String userId,long fileSize,String email) {
        // NOTE: The log message was slightly off; fixed it below.
        String message = String.format("{\"fileName\":\"%s\", \"fileType\":\"%s\", \"s3Location\":\"%s\", \"userId\":\"%s\",\"fileSize\":%d,\"email\":\"%s\"}",
                fileName, fileType, s3Location, userId, fileSize, email);
        log.info("Published metadata request for file: {}", fileName);
        kafkaTemplate.send(metadataTopic, message);
    }

    /**
     * Publishes a BanNotification object to Kafka by serializing it to a JSON string.
     */
    public void publishBanNotification(BanNotification banNotification){
        try {
            String jsonMessage = objectMapper.writeValueAsString(banNotification);
            log.warn("Published ban notification to Kafka for user: {}", banNotification.getEmail());
            kafkaTemplate.send(notificationTopic, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize BanNotification object: {}", banNotification.getEmail(), e);
            // Handle serialization error appropriately (e.g., retry or log)
        }
    }
}