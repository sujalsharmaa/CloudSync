package com.search_service.search_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; // If using a separate class
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration // Add this if creating a new file like KafkaConfig.java
public class KafkaConfig {

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        // 1. Create a recoverer that publishes failed messages to "topic.DLT"
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        // 2. Configure the error handler
        // Retries 3 times, waiting 1 second between attempts, then sends to DLQ.
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}