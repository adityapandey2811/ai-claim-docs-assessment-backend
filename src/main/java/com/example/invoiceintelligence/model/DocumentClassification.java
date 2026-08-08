package com.example.invoiceintelligence.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentClassification {
    private String documentId;
    private String documentType;
    private double confidence;
    private String reasoning;
}
