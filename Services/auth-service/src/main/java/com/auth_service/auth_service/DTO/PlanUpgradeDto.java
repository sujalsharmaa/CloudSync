package com.auth_service.auth_service.DTO;

import com.auth_service.auth_service.Entity.type.Plan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanUpgradeDto {
    private Long userId;
    private Plan plan;
}
