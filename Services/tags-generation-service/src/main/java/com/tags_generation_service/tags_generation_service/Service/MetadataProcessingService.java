package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataProcessingService {

    private final Map<String, ChatLanguageModel> specializedModels;
    private final EmbeddingModel embeddingModel;
    private final S3Service s3Service;
    private final PostgresService postgresService;
    private final QueueService queueService;
    private final Tika tika = new Tika();
    private final FileMetadataPostgresRepository fileMetadataPostgresRepository;

    /**
     * This method would be triggered by a message queue listener.
     */
    public void processMetadataRequest(String fileName, String fileType, String s3Location, String userId, Long fileSize,String email) {
        try {
            log.info("Processing metadata for file: {} from S3 location: {}", fileName, s3Location);

            InputStream fileStream = s3Service.downloadFile(s3Location);

            String content = "";
            Map<String, Object> analysis;
            ChatLanguageModel llm = selectSpecializedLLM(fileType);

            if ("image".equals(fileType)) {
                analysis = analyzeImage(llm, fileStream);
            } else {
                content = extractContent(fileStream);
                analysis = analyzeDocument(llm, content, fileType);
            }

            // 1. Extract analysis results
            List<String> tags = (List<String>) analysis.get("tags");
            List<String> categories = (List<String>) analysis.get("categories");
            String summary = (String) analysis.get("summary");

            // --- Core Fix Starts Here ---

            FileMetadataPostgres postgresMetadata;
            Optional<FileMetadataPostgres> existingFile = Optional.ofNullable(fileMetadataPostgresRepository.findByFileName(fileName));

            if (existingFile.isPresent()) {
                // Update existing file
                postgresMetadata = existingFile.get();

                // Update fields
                postgresMetadata.setS3Location(s3Location);
                postgresMetadata.setModifiedAt(new Date());
                postgresMetadata.setTags(tags);          // Use the List<String> directly
                postgresMetadata.setCategories(categories); // Use the List<String> directly
                postgresMetadata.setSummary(summary);
                postgresMetadata.setFileSize(fileSize);

                // 3. Save the actual object, not the Optional
                postgresService.saveOrUpdateMetadata(postgresMetadata);

                // 4. CQRS - Publish the actual object
                queueService.publishFileRequest(postgresMetadata);

            } else {
                // Create new file
                // 2. CONVERT FileMetadata to FileMetadataPostgres for the database

                postgresMetadata = FileMetadataPostgres.builder()
                        .fileName(fileName)
                        .fileType(fileType)
                        .tags(tags) // Use the List<String> directly
                        .categories(categories) // Use the List<String> directly
                        .summary(summary)
                        .s3Location(s3Location)
                        .processedAt(new Date())
                        .userId(userId)
                        .isMovedToRecycleBin(false)
                        .isStarred(false) // Initialize isStarred to false
                        .fileSize(fileSize)
                        .email(email)
                        .build();

                postgresService.saveOrUpdateMetadata(postgresMetadata);
                // 4. CQRS - Publish the actual object
                queueService.publishFileRequest(postgresMetadata);
            }

            log.info("Metadata for file {} saved successfully to PostgreSQL and Elasticsearch.", fileName);

        } catch (Exception e) {
            log.error("Error processing metadata for file: {}", fileName, e);
            // Implement robust error handling (e.g., dead-letter queue)
        }
    }

    // All the private LLM analysis methods are moved here
    private String extractContent(InputStream stream) throws Exception {
        return tika.parseToString(stream);
    }

    private ChatLanguageModel selectSpecializedLLM(String fileType) {
        return specializedModels.getOrDefault(fileType, specializedModels.get("default"));
    }

    private Map<String, Object> analyzeDocument(ChatLanguageModel llm, String content, String fileType) {
        // Your existing logic for tags, categories, and summary
        String truncatedContent = content.length() > 3000 ?
                content.substring(0, 3000) + "..." : content;

        String prompt = String.format("""
            Analyze this %s document and provide:
            1. 5-10 relevant tags (keywords)
            2. 1-3 categories (broad classification)
            3. A brief summary (2-3 sentences)
            
            Content:
            %s

            Format your response as:
            TAGS: tag1, tag2, tag3...
            CATEGORIES: category1, category2...
            SUMMARY: Your summary here
            """, fileType, truncatedContent);

        String response = llm.generate(prompt);
        return parseAnalysisResponse(response);
    }

    private Map<String, Object> analyzeImage(ChatLanguageModel llm, InputStream imageStream) throws Exception {
        // Your existing logic for tags, categories, and summary
        byte[] imageBytes = imageStream.readAllBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        ImageContent imageContent = ImageContent.from(base64Image, "image/png");

        List<Content> contents = new ArrayList<>();
        contents.add(imageContent);
        contents.add(TextContent.from("Analyze the provided image and describe its contents. Then provide: " +
                "1. 2-5 relevant tags (keywords), " +
                "2. 1-3 categories (broad classification), " +
                "3. A brief summary (2-3 sentences). " +
                "Format your response as: TAGS: ..., CATEGORIES: ..., SUMMARY: ..."));
        UserMessage userMessage = UserMessage.from(contents);

        String response = llm.generate(userMessage).content().text();
        return parseAnalysisResponse(response);
    }

    private Map<String, Object> parseAnalysisResponse(String response) {
        // Your existing parsing logic for tags, categories, and summary
        Map<String, Object> result = new HashMap<>();
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.startsWith("TAGS:")) {
                String tags = line.substring(5).trim();
                result.put("tags", Arrays.asList(tags.split(",\\s*")));
            } else if (line.startsWith("CATEGORIES:")) {
                String categories = line.substring(11).trim();
                result.put("categories", Arrays.asList(categories.split(",\\s*")));
            } else if (line.startsWith("SUMMARY:")) {
                result.put("summary", line.substring(8).trim());
            }
        }
        result.putIfAbsent("tags", List.of("untagged"));
        result.putIfAbsent("categories", List.of("uncategorized"));
        result.putIfAbsent("summary", "No summary available");
        return result;
    }
}
