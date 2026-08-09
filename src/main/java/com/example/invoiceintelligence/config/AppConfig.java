package com.example.invoiceintelligence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${app.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.tesseract.data-path:}")
    private String tessDataPath;

    public String getTessDataPath() {
        return tessDataPath;
    }

    public String[] getAllowedOrigins() {
        return allowedOrigins.split(",");
    }
}