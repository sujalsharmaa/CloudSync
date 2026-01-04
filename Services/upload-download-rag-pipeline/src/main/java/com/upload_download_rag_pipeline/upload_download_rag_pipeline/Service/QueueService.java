package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.BanNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.metadata:file-metadata-requests}")
    private String metadataTopic;

    @Value("${kafka.topics.notification:notification-topic}")
    private String notificationTopic;

    public void publishMetadataRequest(String fileName, String fileType, String s3Location, String userId, long fileSize, String email) {
        MetadataRequest request = new MetadataRequest(fileName, fileType, s3Location, userId, fileSize, email);
        publishToTopic(metadataTopic, request, "metadata request");
    }

    public void publishBanNotification(BanNotification banNotification) {
        publishToTopic(notificationTopic, banNotification, "ban notification");
    }

    private void publishToTopic(String topic, Object payload, String logDescription) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(payload);
            log.info("Publishing {} to topic '{}': {}", logDescription, topic, payload);
            kafkaTemplate.send(topic, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for {}: {}", logDescription, payload, e);
        }
    }

    private record MetadataRequest(String fileName, String fileType, String s3Location, String userId, long fileSize, String email) {}
}