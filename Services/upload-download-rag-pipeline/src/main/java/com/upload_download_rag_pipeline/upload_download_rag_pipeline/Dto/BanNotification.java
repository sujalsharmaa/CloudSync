package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto;

// BanNotification.java
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BanNotification {
    private String userId;
    private String email;
    private String username;
    private String banDuration; // e.g., "24 hours", "1 month", "Lifetime"
    private String banReason;   // e.g., "Policy violation count reached X"
}