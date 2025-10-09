package com.auth_service.auth_service.DTO;

import com.auth_service.auth_service.Entity.type.Plan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for publishing a user's plan upgrade information to Kafka.
 * This structure must match the DTO used by the consuming service (auth_service).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanUpgradeDto {
    private Long userId;
    private Plan plan;
}
