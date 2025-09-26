package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class MCPResponse {
    private boolean success;
    private Object data;
    private String error;
    private long timestamp;

    private static final ObjectMapper mapper = new ObjectMapper();

    public static MCPResponse success(Object data) {
        MCPResponse response = new MCPResponse();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static MCPResponse error(String error) {
        MCPResponse response = new MCPResponse();
        response.setSuccess(false);
        response.setError(error);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\":\"Failed to serialize response\"}";
        }
    }
}