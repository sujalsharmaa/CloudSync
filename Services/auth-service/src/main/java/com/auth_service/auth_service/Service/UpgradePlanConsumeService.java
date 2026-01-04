package com.auth_service.auth_service.Service;

import com.auth_service.auth_service.DTO.PlanUpgradeDto;
import com.auth_service.auth_service.Entity.type.User;
import com.auth_service.auth_service.Repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper; // Required for JSON parsing
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Good practice for database updates
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpgradePlanConsumeService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper; // Inject ObjectMapper for JSON processing

    @KafkaListener(topics = "user-plan-upgrade")
    @Transactional
    public void listen(String message) throws Exception {
        log.info("Received message from Kafka: {}", message);
            PlanUpgradeDto planUpgradeDto = objectMapper.readValue(message, PlanUpgradeDto.class);
            Optional<User> userOptional = userRepository.findById(planUpgradeDto.getUserId());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPlan(planUpgradeDto.getPlan());
                userRepository.save(user);

                log.info("Successfully upgraded plan for user ID {} to {}", user.getId(), user.getPlan());
            } else {
                log.warn("User with ID {} not found for plan upgrade.", planUpgradeDto.getUserId());
            }

    }
}
