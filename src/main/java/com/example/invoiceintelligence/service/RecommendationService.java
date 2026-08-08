package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.client.GeminiClient;
import com.example.invoiceintelligence.model.ClaimRecommendation;
import com.example.invoiceintelligence.model.Discrepancy;
import com.example.invoiceintelligence.model.DocumentClassification;
import com.example.invoiceintelligence.model.DocumentValidation;
import com.example.invoiceintelligence.model.UploadedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private final GeminiClient geminiClient;

    public RecommendationService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public ClaimRecommendation recommend(
            List<UploadedDocument> documents,
            List<DocumentClassification> classifications,
            List<DocumentValidation> validations,
            List<Discrepancy> discrepancies
    ) {
        log.info("Generating claim recommendation for {} documents", documents.size());
        ClaimRecommendation recommendation = geminiClient.recommendClaim(documents, classifications, validations, discrepancies);
        recommendation.setDisclaimer("This is an AI-generated recommendation for human review only. Final claim decisions must be made by an authorized adjuster.");
        log.info("Recommendation generated: {}", recommendation.getRecommendation());
        return recommendation;
    }
}
