package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BanNotification {
    private String userId;
    private String email;
    private String username;
    private String banDuration;
    private String banReason;
}