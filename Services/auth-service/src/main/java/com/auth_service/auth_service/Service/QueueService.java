package com.auth_service.auth_service.Service;


import com.auth_service.auth_service.DTO.WelcomeEmailNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final String metadataTopic = "welcome-email-topic";

    public void publishWelcomeEmailRequest(WelcomeEmailNotification welcomeEmailNotification) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(welcomeEmailNotification);
            log.info("Published message to Kafka topic '{}' for email: {}", metadataTopic,welcomeEmailNotification.getEmail());
            kafkaTemplate.send(metadataTopic, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize FileMetadataPostgres to JSON", e);
        }
    }
}