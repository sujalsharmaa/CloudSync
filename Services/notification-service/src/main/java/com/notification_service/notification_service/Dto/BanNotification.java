package com.notification_service.notification_service.Dto;

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
    private String banDuration;
    private String banReason;
}