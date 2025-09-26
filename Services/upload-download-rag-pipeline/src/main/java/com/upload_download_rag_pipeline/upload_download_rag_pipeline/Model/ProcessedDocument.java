package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedDocument {
    private String fileName;
    private String fileType;
    private List<String> tags;
    private List<String> categories;
    private String summary;
    private String securityStatus;
    private String rejectionReason;
    private String s3Location;
    private String userId;
    private long fileSize;
}