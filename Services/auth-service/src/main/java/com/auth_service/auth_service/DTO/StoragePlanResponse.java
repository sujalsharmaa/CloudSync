package com.auth_service.auth_service.DTO;

import com.auth_service.auth_service.Entity.type.Plan;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class StoragePlanResponse {
    private Plan plan;
    private Long StorageConsumed;
}
