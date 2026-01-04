package com.tags_generation_service.tags_generation_service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataConsumerService {

    private final MetadataProcessingService metadataProcessingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "file-metadata-requests", groupId = "rag-pipeline-group")
    public void listen(String message) throws Exception {
        log.info("Received message from Kafka: {}", message);
            Map<String, Object> map = objectMapper.readValue(message, Map.class);

            String fileName = (String) map.get("fileName");
            String fileType = (String) map.get("fileType");
            String s3Location = (String) map.get("s3Location");
            String userId = (String) map.get("userId");
            Long fileSize = ((Number) map.get("fileSize")).longValue();
            String email = (String) map.get("email");

            log.info("Processing file metadata from topic 'file-metadata-requests': {}", fileName);

            metadataProcessingService.processMetadataRequest(fileName, fileType, s3Location, userId, fileSize, email);

    }
}