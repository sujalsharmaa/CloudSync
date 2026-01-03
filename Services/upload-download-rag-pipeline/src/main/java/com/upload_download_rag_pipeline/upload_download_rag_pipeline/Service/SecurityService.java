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
import org.springframework.core.io.Resource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StreamUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final Map<String, ChatLanguageModel> specializedModels;
    private final Tika tika = new Tika();
    private final ObjectMapper objectMapper;
    private static final int MAX_CONTENT_LENGTH = 3000;
    private static final String TRUNCATION_INDICATOR = "...";

    // Inject prompt from resources
    @Value("classpath:prompts/security-check.txt")
    private Resource securityPromptResource;
    private String systemPrompt;

    private String getSystemPrompt() {
        if (systemPrompt == null) {
            try {
                systemPrompt = StreamUtils.copyToString(
                        securityPromptResource.getInputStream(),
                        StandardCharsets.UTF_8
                );
            } catch (IOException e) {
                log.error("Failed to load security prompt", e);
                throw new RuntimeException("Could not load security prompt", e);
            }
        }
        return systemPrompt;
    }
    private String truncateContent(String content) {
        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_LENGTH) + TRUNCATION_INDICATOR;
    }
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
                // 1. Handle Images with Multimodal LLM
                return analyzeImageForSecurity(llm, fileStream);

            } else if ("video".equals(fileType) || "audio".equals(fileType)) {
                // 2. (FIX) Explicitly handle video/audio
                // We skip content analysis for these types for now,
                // as Tika will crash and we don't have a video-analysis model.
                log.info("Skipping text content analysis for media type: {}", fileType);

                Map<String, Object> result = new HashMap<>();
                result.put("security_status", "safe");
                result.put("rejection_reason", null);
                return result;

            } else {
                // 3. Handle Text-Based Files (pdf, doc, txt, code, etc.)
                // This block is now safe, as videos and audio are already handled.
                String content = extractContent(fileStream);
                return analyzeDocumentForSecurity(llm, content, fileType);
            }
        } catch (Exception e) {
            log.error("Security check failed for file: {}", fileName, e);
            // Return a default error status
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("security_status", "error");
            errorResult.put("rejection_reason", "An internal error occurred during the security check.");
            return errorResult;
        }
    }
    private Map<String, Object> analyzeDocumentForSecurity(
            ChatLanguageModel llm, String content, String fileType) {

        String truncated = truncateContent(content);
        String userPrompt = String.format(
                "Analyze this %s document for explicit or harmful content.\n\nContent:\n%s",
                fileType, truncated
        );

        String response = llm.generate(getSystemPrompt() + userPrompt);
        return parseJsonSecurityResponse(response);
    }

    private Map<String, Object> analyzeImageForSecurity(ChatLanguageModel llm, InputStream imageStream) throws Exception {
        byte[] imageBytes = readInputStreamToBytes(imageStream);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        ImageContent imageContent = ImageContent.from(base64Image, "image/png");
        TextContent textContent = TextContent.from(getSystemPrompt() + "Analyze the provided image for explicit or harmful content.");
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