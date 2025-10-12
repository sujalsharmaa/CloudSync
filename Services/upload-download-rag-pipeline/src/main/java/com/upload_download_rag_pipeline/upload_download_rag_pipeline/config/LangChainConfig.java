package com.upload_download_rag_pipeline.upload_download_rag_pipeline.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class LangChainConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChainConfig.class);

    @Value("${genai.gemini.api-key}")
    private String geminiKey;

    @Value("${genai.gemini.model.text}")
    private String textModel;

    @Value("${genai.gemini.model.code}")
    private String codeModel;

    @Value("${genai.gemini.model.image}")
    private String imageModel;

    // ADDED: Configuration for the video model
    @Value("${genai.gemini.model.video}")
    private String videoModel;

    @Value("${genai.gemini.model.embedding}")
    private String embeddingModel;

    @Bean
    public Map<String, ChatLanguageModel> specializedModels() {
        Map<String, ChatLanguageModel> models = new HashMap<>();

        // Text Model
        models.put("text", GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName(textModel)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(60))
                .build());

        // Code Model
        models.put("code", GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName(codeModel)
                .temperature(0.1)
                .timeout(Duration.ofSeconds(60))
                .build());

        // Image Model
        models.put("image", GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName(imageModel)
                .temperature(0.5)
                .timeout(Duration.ofSeconds(60))
                .build());

        // ADDED: Video Model
        // Assuming video RAG might need a slightly higher temperature for creative summaries/analysis
        models.put("video", GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName(videoModel) // Uses the new videoModel value
                .temperature(0.6) // A slightly higher temperature is often useful for complex analysis
                .timeout(Duration.ofSeconds(60))
                .build());

        // Default Model
        models.put("default", GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName(textModel)
                .temperature(0.4)
                .timeout(Duration.ofSeconds(60))
                .build());

        return models;
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiKey)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}