package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.client.GeminiClient;
import com.example.invoiceintelligence.model.DocumentClassification;
import com.example.invoiceintelligence.model.DocumentValidation;
import com.example.invoiceintelligence.model.UploadedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentValidatorService {

    private static final Logger log = LoggerFactory.getLogger(DocumentValidatorService.class);
    private final GeminiClient geminiClient;

    public DocumentValidatorService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public DocumentValidation validate(UploadedDocument document, DocumentClassification classification) {
        log.info("Validating document {} of type {}", document.getId(), classification.getDocumentType());
        DocumentValidation validation = geminiClient.validateDocument(document.getExtractedText(), classification);
        validation.setDocumentId(document.getId());
        log.info("Document {} validation result: {}", document.getId(), validation.isValid());
        return validation;
    }

    public List<DocumentValidation> validateDocuments(List<UploadedDocument> documents, List<DocumentClassification> classifications) {
        return documents.stream()
                .map(document -> {
                    DocumentClassification classification = classifications.stream()
                            .filter(c -> c.getDocumentId().equals(document.getId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Missing classification for document " + document.getId()));
                    return validate(document, classification);
                })
                .collect(Collectors.toList());
    }
}
