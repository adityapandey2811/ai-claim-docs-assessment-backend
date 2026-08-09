package com.example.invoiceintelligence.config;

import com.example.invoiceintelligence.ai.ClaimQuestionAssistant;
import com.example.invoiceintelligence.ai.CrossCheckAssistant;
import com.example.invoiceintelligence.ai.DocumentClassifierAssistant;
import com.example.invoiceintelligence.ai.DocumentValidatorAssistant;
import com.example.invoiceintelligence.ai.RecommendationAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicitly builds every LangChain4j AiServices proxy bean, all backed by the
 * single geminiChatLanguageModel bean from GeminiChatModelConfig. This is intentionally
 * manual (not using the langchain4j-spring-boot-starter's @AiService scanning) so
 * every bean is visible and traceable here - no annotation-driven magic that can
 * silently fail to wire up.
 */
@Configuration
public class AiServiceFactoryConfig {

    @Bean
    public DocumentClassifierAssistant documentClassifierAssistant(ChatLanguageModel geminiChatModel) {
        return AiServices.builder(DocumentClassifierAssistant.class)
                .chatLanguageModel(geminiChatModel)
                .build();
    }

    @Bean
    public DocumentValidatorAssistant documentValidatorAssistant(ChatLanguageModel geminiChatModel) {
        return AiServices.builder(DocumentValidatorAssistant.class)
                .chatLanguageModel(geminiChatModel)
                .build();
    }

    @Bean
    public CrossCheckAssistant crossCheckAssistant(ChatLanguageModel geminiChatModel) {
        return AiServices.builder(CrossCheckAssistant.class)
                .chatLanguageModel(geminiChatModel)
                .build();
    }

    @Bean
    public RecommendationAssistant recommendationAssistant(ChatLanguageModel geminiChatModel) {
        return AiServices.builder(RecommendationAssistant.class)
                .chatLanguageModel(geminiChatModel)
                .build();
    }

    @Bean
    public ClaimQuestionAssistant claimQuestionAssistant(ChatLanguageModel geminiChatModel) {
        return AiServices.builder(ClaimQuestionAssistant.class)
                .chatLanguageModel(geminiChatModel)
                .build();
    }
}