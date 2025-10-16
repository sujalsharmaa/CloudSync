package com.notification_service.notification_service.Dto;

// BanNotification.java
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanNotification {
    private String userId;
    private String email;
    private String username;
    private String banDuration; // e.g., "24 hours", "1 month", "Lifetime"
    private String banReason;   // e.g., "Policy violation count reached X"
}