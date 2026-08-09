package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.ai.RecommendationAssistant;
import com.example.invoiceintelligence.model.ClaimRecommendation;
import com.example.invoiceintelligence.model.Discrepancy;
import com.example.invoiceintelligence.model.DocumentClassification;
import com.example.invoiceintelligence.model.DocumentValidation;
import com.example.invoiceintelligence.model.UploadedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private final RecommendationAssistant recommendationAssistant;

    public RecommendationService(RecommendationAssistant recommendationAssistant) {
        this.recommendationAssistant = recommendationAssistant;
    }

    public ClaimRecommendation recommend(
            List<UploadedDocument> documents,
            List<DocumentClassification> classifications,
            List<DocumentValidation> validations,
            List<Discrepancy> discrepancies
    ) {
        log.info("Generating claim recommendation for {} documents", documents.size());

        String documentsSummary = buildDocumentsSummary(documents, classifications);
        String discrepanciesSummary = buildDiscrepanciesSummary(discrepancies);

        ClaimRecommendation recommendation =
                recommendationAssistant.recommend(documentsSummary, discrepanciesSummary);

        if (recommendation.getDisclaimer() == null || recommendation.getDisclaimer().isEmpty()) {
            recommendation.setDisclaimer(
                    "This is an AI-generated recommendation for human review only. " +
                    "Final claim decisions must be made by an authorized adjuster.");
        }

        log.info("Recommendation generated: {}", recommendation.getRecommendation());
        return recommendation;
    }

    private String buildDocumentsSummary(
            List<UploadedDocument> documents, List<DocumentClassification> classifications) {

        Map<String, DocumentClassification> classificationByDocId = classifications.stream()
                .collect(Collectors.toMap(DocumentClassification::getDocumentId, c -> c));

        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (UploadedDocument document : documents) {
            DocumentClassification classification = classificationByDocId.get(document.getId());
            String documentType = classification != null ? classification.getDocumentType() : "UNKNOWN";

            sb.append("--- Document ").append(index).append(" (").append(documentType).append(") ---\n");
            sb.append(document.getExtractedText()).append("\n\n");
            index++;
        }
        return sb.toString();
    }

    private String buildDiscrepanciesSummary(List<Discrepancy> discrepancies) {
        if (discrepancies == null || discrepancies.isEmpty()) {
            return "No discrepancies were found during cross-document consistency checking.";
        }

        StringBuilder sb = new StringBuilder();
        for (Discrepancy d : discrepancies) {
            sb.append("- [").append(d.getSeverity()).append("] ")
              .append(d.getField()).append(": ").append(d.getDescription())
              .append(" (documents: ").append(d.getDocumentsInvolved()).append(")\n");
        }
        return sb.toString();
    }
}