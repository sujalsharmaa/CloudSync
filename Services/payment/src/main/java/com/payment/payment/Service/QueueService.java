// tags-generation-service/Service/QueueService.java
// (no changes, just for context)
package com.payment.payment.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment.Dto.PlanUpgradeDto;
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
    private final String metadataTopic = "user-plan-upgrade";

    public void publishPlanUpgradeInfo(PlanUpgradeDto planUpgradeDto) {
        try {
            System.out.println("we received a request to publish ");
            String jsonMessage = objectMapper.writeValueAsString(planUpgradeDto);
            log.info("User Id "+planUpgradeDto.getUserId()+" is upgraded to plan "+planUpgradeDto.getPlan());
            kafkaTemplate.send(metadataTopic, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish updated plan", e);
        }
    }
}