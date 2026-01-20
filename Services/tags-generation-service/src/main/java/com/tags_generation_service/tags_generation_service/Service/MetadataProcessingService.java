package com.tags_generation_service.tags_generation_service.Service;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Repository.FileMetadataPostgresRepository;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataProcessingService {

    private final Map<String, ChatLanguageModel> specializedModels;
    private final S3Service s3Service;
    private final PostgresService postgresService;
    private final QueueService queueService;
    private final FileMetadataPostgresRepository repository;

    private static final int MAX_CONTENT_LENGTH = 3000;
    private static final String TRUNCATION_INDICATOR = "...";

    private final Tika tika = new Tika();

    @Value("classpath:Prompts/metadata-analysis-text.txt")
    private Resource textPromptResource;

    @Value("classpath:Prompts/metadata-analysis-image.txt")
    private Resource imagePromptResource;

    private String systemPromptText;
    private String systemPromptImage;


    @PostConstruct
    void loadPrompts() {
        try {
            systemPromptText = StreamUtils.copyToString(
                    textPromptResource.getInputStream(), StandardCharsets.UTF_8);

            systemPromptImage = StreamUtils.copyToString(
                    imagePromptResource.getInputStream(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load LLM prompts", e);
        }
    }


    public void processMetadataRequest(
            String fileName,
            String fileType,
            String s3Location,
            String userId,
            Long fileSize,
            String email
    ) {
        try (InputStream fileStream = s3Service.downloadFile(s3Location)) {

            ChatLanguageModel llm = selectLLM(fileType);
            Map<String, Object> analysis;

            if ("image".equalsIgnoreCase(fileType)) {
                analysis = analyzeImage(llm, fileStream);
            } else {
                String content = extractContent(fileStream);
                analysis = analyzeDocument(llm, content, fileType);
            }

            saveMetadata(fileName, fileType, s3Location, userId, fileSize, email, analysis);

        } catch (Exception e) {
            log.error("Metadata processing failed for file {}", fileName, e);
        }
    }


    private void saveMetadata(
            String fileName,
            String fileType,
            String s3Location,
            String userId,
            Long fileSize,
            String email,
            Map<String, Object> analysis
    ) {
        List<String> tags = safeList(analysis.get("tags"));
        List<String> categories = safeList(analysis.get("categories"));
        String summary = Objects.toString(analysis.get("summary"));

        FileMetadataPostgres metadata =
                Optional.ofNullable(repository.findByFileName(fileName))
                        .map(existing -> {
                            existing.setTags(tags);
                            existing.setCategories(categories);
                            existing.setSummary(summary);
                            existing.setFileSize(fileSize);
                            existing.setModifiedAt(new Date());
                            existing.setS3Location(s3Location);
                            return existing;
                        })
                        .orElse(FileMetadataPostgres.builder()
                                .fileName(fileName)
                                .fileType(fileType)
                                .tags(tags)
                                .categories(categories)
                                .summary(summary)
                                .s3Location(s3Location)
                                .userId(userId)
                                .email(email)
                                .fileSize(fileSize)
                                .processedAt(new Date())
                                .isStarred(false)
                                .isMovedToRecycleBin(false)
                                .build());

        postgresService.saveOrUpdateMetadata(metadata);
        queueService.publishFileRequest(metadata);
    }


    private Map<String, Object> analyzeDocument(ChatLanguageModel llm, String content, String fileType) {
        String truncated = truncate(content);
        String prompt = String.format(systemPromptText, fileType, truncated);
        return parseResponse(llm.generate(prompt));
    }

    private Map<String, Object> analyzeImage(ChatLanguageModel llm, InputStream stream) throws Exception {
        byte[] bytes = stream.readAllBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);

        UserMessage message = UserMessage.from(
                ImageContent.from(base64, "image/*"),
                TextContent.from(systemPromptImage)
        );

        return parseResponse(llm.generate(message).content().text());
    }


    private ChatLanguageModel selectLLM(String fileType) {
        return Optional.ofNullable(specializedModels.get(fileType))
                .orElseGet(() -> specializedModels.get("default"));
    }

    private String extractContent(InputStream stream) throws Exception {
        return tika.parseToString(stream);
    }

    private String truncate(String content) {
        return content.length() <= MAX_CONTENT_LENGTH
                ? content
                : content.substring(0, MAX_CONTENT_LENGTH) + TRUNCATION_INDICATOR;
    }

    private Map<String, Object> parseResponse(String response) {
        Map<String, Object> result = new HashMap<>();

        for (String line : response.split("\n")) {
            if (line.startsWith("TAGS:"))
                result.put("tags", Arrays.asList(line.substring(5).split(",\\s*")));
            else if (line.startsWith("CATEGORIES:"))
                result.put("categories", Arrays.asList(line.substring(11).split(",\\s*")));
            else if (line.startsWith("SUMMARY:"))
                result.put("summary", line.substring(8).trim());
        }

        result.putIfAbsent("tags", List.of("untagged"));
        result.putIfAbsent("categories", List.of("uncategorized"));
        result.putIfAbsent("summary", "No summary available");

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> safeList(Object value) {
        return value instanceof List ? (List<String>) value : List.of();
    }
}
