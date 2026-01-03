//package com.tags_generation_service.tags_generation_service.Service;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.configurationprocessor.json.JSONObject;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class MetadataConsumerService {
//
//    private final MetadataProcessingService metadataProcessingService;
//
//    /**
//     * This method listens for new messages on the Kafka topic and processes them.
//     */
//    @KafkaListener(topics = "file-metadata-requests", groupId = "rag-pipeline-group")
//    public void listen(String message) {
//        log.info("Received message from Kafka: {}", message);
//        try {
//            JSONObject json = new JSONObject(message);
//            String fileName = json.getString("fileName");
//            String fileType = json.getString("fileType");
//            String s3Location = json.getString("s3Location");
//            String userId = json.getString("userId");
//            Long fileSize = json.getLong("FileSize");
//
//            log.info("we got the file metadata on topic "+ "file-metadata-requests"+ fileSize);
//
//            // Trigger the core processing logic
//            metadataProcessingService.processMetadataRequest(fileName, fileType, s3Location,userId,fileSize);
//
//        } catch (Exception e) {
//            log.error("Error processing Kafka message: {}", message, e);
//            // In a real-world scenario, you would handle this gracefully, e.g.,
//            // by pushing it to a dead-letter topic.
//        }
//    }
//}
