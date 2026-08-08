package com.example.invoiceintelligence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.gemini.endpoint}")
    private String geminiEndpoint;

    @Value("${app.gemini.request-timeout-seconds:60}")
    private int requestTimeoutSeconds;

    @Value("${app.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.tesseract.data-path:}")
    private String tessDataPath;

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getGeminiEndpoint() {
        return geminiEndpoint;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public String getTessDataPath() {
        return tessDataPath;
    }

    public String[] getAllowedOrigins() {
        return allowedOrigins.split(",");
    }
}
