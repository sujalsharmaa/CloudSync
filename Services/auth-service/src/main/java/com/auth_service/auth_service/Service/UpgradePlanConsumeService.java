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
            // Fix 1 & 2: Use ObjectMapper to correctly map the JSON string to the DTO
            PlanUpgradeDto planUpgradeDto = objectMapper.readValue(message, PlanUpgradeDto.class);

            // Fix 3: Use findById for a single ID lookup (resolves Iterable vs Long error)
            Optional<User> userOptional = userRepository.findById(planUpgradeDto.getUserId());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPlan(planUpgradeDto.getPlan());


                // Fix 4: Save the updated user object to persist the plan change
                userRepository.save(user);

                log.info("Successfully upgraded plan for user ID {} to {}", user.getId(), user.getPlan());
            } else {
                log.warn("User with ID {} not found for plan upgrade.", planUpgradeDto.getUserId());
            }

    }
}
