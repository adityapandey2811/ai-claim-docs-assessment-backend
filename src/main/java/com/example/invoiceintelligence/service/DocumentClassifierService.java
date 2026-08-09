package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.ai.DocumentClassifierAssistant;
import com.example.invoiceintelligence.model.DocumentClassification;
import com.example.invoiceintelligence.model.UploadedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentClassifierService {

    private static final Logger log = LoggerFactory.getLogger(DocumentClassifierService.class);
    private final DocumentClassifierAssistant documentClassifierAssistant;

    public DocumentClassifierService(DocumentClassifierAssistant documentClassifierAssistant) {
        this.documentClassifierAssistant = documentClassifierAssistant;
    }

    public DocumentClassification classify(UploadedDocument document) {
        log.info("Classifying document {} ({})", document.getId(), document.getFileName());
        DocumentClassification classification = documentClassifierAssistant.classify(document.getExtractedText());
        classification.setDocumentId(document.getId());
        log.info("Document {} classified as {} with confidence {}",
                document.getId(), classification.getDocumentType(), classification.getConfidence());
        return classification;
    }

    public List<DocumentClassification> classifyDocuments(List<UploadedDocument> documents) {
        return documents.stream()
                .map(this::classify)
                .collect(Collectors.toList());
    }
}