package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import ch.qos.logback.core.util.FileSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String metadataTopic = "file-metadata-requests";

    /**
     * Publishes a message to a Kafka topic to trigger asynchronous metadata processing.
     * The message contains a JSON string with the file details.
     */
    public void publishMetadataRequest(String fileName, String fileType, String s3Location, String userId,long fileSize) {
        String message = String.format("{\"fileName\":\"%s\", \"fileType\":\"%s\", \"s3Location\":\"%s\", \"userId\":\"%s\",\"FileSize\":\"%s\"}", fileName, fileType, s3Location,userId,fileSize);
        log.info("Published message to Kafka topic '{}' for file: {}", metadataTopic, fileName,fileSize);
        kafkaTemplate.send(metadataTopic, message);
    }
}