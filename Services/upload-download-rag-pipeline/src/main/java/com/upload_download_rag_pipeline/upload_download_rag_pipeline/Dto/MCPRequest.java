package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MCPRequest {
    private String action;
    private String query;
    private String fileId;
    private List<String> tags;
    private String category;
    private Integer limit = 10;
    private Map<String, Object> parameters;

    private static final ObjectMapper mapper = new ObjectMapper();

    public static MCPRequest fromJson(String json) {
        try {
            return mapper.readValue(json, MCPRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse MCP request", e);
        }
    }
}

