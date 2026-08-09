package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.ai.CrossCheckAssistant;
import com.example.invoiceintelligence.ai.DiscrepancyReport;
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
public class CrossCheckService {

    private static final Logger log = LoggerFactory.getLogger(CrossCheckService.class);
    private final CrossCheckAssistant crossCheckAssistant;

    public CrossCheckService(CrossCheckAssistant crossCheckAssistant) {
        this.crossCheckAssistant = crossCheckAssistant;
    }

    public List<Discrepancy> checkConsistency(
            List<UploadedDocument> documents,
            List<DocumentClassification> classifications,
            List<DocumentValidation> validations
    ) {
        log.info("Running cross-document consistency check for {} documents", documents.size());

        String summary = buildDocumentsSummary(documents, classifications);
        log.debug("Cross-check prompt input:\n{}", summary);

        DiscrepancyReport report = crossCheckAssistant.crossCheck(summary);
        List<Discrepancy> discrepancies =
                report != null && report.getDiscrepancies() != null ? report.getDiscrepancies() : List.of();

        log.info("Cross-check produced {} discrepancy items", discrepancies.size());
        return discrepancies;
    }

    /**
     * Formats all documents + their classifications into a single readable text
     * block for the LLM. Kept as plain text (not JSON) deliberately so it's easy
     * to log and show during a demo exactly what was sent to Gemini.
     */
    private String buildDocumentsSummary(
            List<UploadedDocument> documents, List<DocumentClassification> classifications) {

        Map<String, DocumentClassification> classificationByDocId = classifications.stream()
                .collect(Collectors.toMap(DocumentClassification::getDocumentId, c -> c));

        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (UploadedDocument document : documents) {
            DocumentClassification classification = classificationByDocId.get(document.getId());
            String documentType = classification != null ? classification.getDocumentType() : "UNKNOWN";

            sb.append("--- Document ").append(index).append(" (").append(documentType)
              .append(", file: ").append(document.getFileName()).append(") ---\n");
            sb.append(document.getExtractedText()).append("\n\n");
            index++;
        }
        return sb.toString();
    }
}