package com.example.invoiceintelligence.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the single real LangChain4j ChatModel bean backed by Gemini.
 * Every AiServices.builder(...) call in this project should reuse this bean
 * rather than creating its own model instance.
 */
@Configuration
public class GeminiChatModelConfig {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model-name:gemini-3.5-flash-lite}")
    private String modelName;

    @Bean
    public ChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequestsAndResponses(true) // helpful during POC/demo debugging; remove/disable for production
                .build();
    }
}