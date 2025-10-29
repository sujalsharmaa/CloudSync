package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final Map<String, ChatLanguageModel> specializedModels;
    private final Tika tika = new Tika();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // A strict system prompt is crucial for reliable content moderation
    private static final String SYSTEM_PROMPT = """
        You are a content moderation AI. Your sole purpose is to detect and flag any content that is sexually explicit, hateful, violent, illegal, or otherwise harmful.
        You MUST respond with a specific JSON object containing a 'security_status' and a 'rejection_reason'.
        If the content is safe, set 'security_status' to 'safe' and 'rejection_reason' to null.
        If the content is unsafe, set 'security_status' to 'unsafe' and provide a clear, concise 'rejection_reason'.
        
        Example unsafe response:
        {
          "security_status": "unsafe",
          "rejection_reason": "Contains sexually explicit material."
        }
        
        Example safe response:
        {
          "security_status": "safe",
          "rejection_reason": null
        }
        
        Do NOT include any other text, explanations, or conversational remarks in your response.
        """;

    // Create a POJO to represent the LLM's JSON response
    private static class SecurityResponse {
        public String security_status;
        public String rejection_reason;
    }

    public Map<String, Object> checkFileSecurity(InputStream fileStream, String fileName, String fileType) throws Exception {
        log.info("Performing security check for file: {} of type: {}", fileName, fileType);
        ChatLanguageModel llm = selectSpecializedLLM(fileType);

        try {
            if ("image".equals(fileType)) {
                return analyzeImageForSecurity(llm, fileStream);
            } else {
                String content = extractContent(fileStream);
                return analyzeDocumentForSecurity(llm, content, fileType);
            }
        } catch (Exception e) {
            log.error("Security check failed for file: {}", fileName, e);
            // Return a default unsafe status in case of an unexpected error
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("security_status", "error");
            errorResult.put("rejection_reason", "An internal error occurred during the security check.");
            return errorResult;
        }
    }

    private Map<String, Object> analyzeDocumentForSecurity(ChatLanguageModel llm, String content, String fileType) {
        String truncatedContent = content.length() > 3000 ?
                content.substring(0, 3000) + "..." : content;

        String userPrompt = String.format("""
            Analyze this %s document for explicit or harmful content.
            
            Content:
            %s
            """, fileType, truncatedContent);

        String response = llm.generate(SYSTEM_PROMPT + userPrompt);
        return parseJsonSecurityResponse(response);
    }

    private Map<String, Object> analyzeImageForSecurity(ChatLanguageModel llm, InputStream imageStream) throws Exception {
        byte[] imageBytes = readInputStreamToBytes(imageStream);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        ImageContent imageContent = ImageContent.from(base64Image, "image/png");
        TextContent textContent = TextContent.from(SYSTEM_PROMPT + "Analyze the provided image for explicit or harmful content.");
        UserMessage userMessage = UserMessage.from(Arrays.asList(textContent, imageContent));

        String response = llm.generate(userMessage).content().text();
        return parseJsonSecurityResponse(response);
    }

    private Map<String, Object> parseJsonSecurityResponse(String response) {
        String sanitizedResponse = response.trim();

        // Check for and remove markdown code block wrappers
        if (sanitizedResponse.startsWith("```json")) {
            sanitizedResponse = sanitizedResponse.substring("```json".length()).trim();
        }
        if (sanitizedResponse.endsWith("```")) {
            sanitizedResponse = sanitizedResponse.substring(0, sanitizedResponse.length() - "```".length()).trim();
        }

        try {
            // Use ObjectMapper to safely and robustly parse the JSON
            SecurityResponse securityResponse = objectMapper.readValue(sanitizedResponse, SecurityResponse.class);

            Map<String, Object> result = new HashMap<>();
            result.put("security_status", securityResponse.security_status);
            result.put("rejection_reason", securityResponse.rejection_reason);

            // Sanity check: if status is not safe or unsafe, default to unsafe
            if (!"safe".equals(result.get("security_status")) && !"unsafe".equals(result.get("security_status"))) {
                log.warn("LLM returned an unexpected status: {}", result.get("security_status"));
                result.put("security_status", "error");
                result.put("rejection_reason", "LLM returned an unexpected status.");
            }
            return result;
        } catch (JsonMappingException e) {
            log.error("Failed to parse LLM response due to invalid JSON format: {}", sanitizedResponse, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("security_status", "error");
            errorResult.put("rejection_reason", "LLM response is not valid JSON.");
            return errorResult;
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", sanitizedResponse, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("security_status", "error");
            errorResult.put("rejection_reason", "An error occurred while parsing LLM response.");
            return errorResult;
        }
    }

    // Helper methods
    private String extractContent(InputStream stream) throws Exception {
        return tika.parseToString(stream);
    }

    private ChatLanguageModel selectSpecializedLLM(String fileType) {
        return specializedModels.getOrDefault(fileType, specializedModels.get("default"));
    }

    private byte[] readInputStreamToBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}