package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.client.GeminiClient;
import com.example.invoiceintelligence.model.Discrepancy;
import com.example.invoiceintelligence.model.DocumentClassification;
import com.example.invoiceintelligence.model.DocumentValidation;
import com.example.invoiceintelligence.model.UploadedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrossCheckService {

    private static final Logger log = LoggerFactory.getLogger(CrossCheckService.class);
    private final GeminiClient geminiClient;

    public CrossCheckService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public List<Discrepancy> checkConsistency(
            List<UploadedDocument> documents,
            List<DocumentClassification> classifications,
            List<DocumentValidation> validations
    ) {
        log.info("Running cross-document consistency check for {} documents", documents.size());
        List<Discrepancy> discrepancies = geminiClient.crossCheckDocuments(documents, classifications, validations);
        log.info("Cross-check produced {} discrepancy items", discrepancies.size());
        return discrepancies;
    }
}
