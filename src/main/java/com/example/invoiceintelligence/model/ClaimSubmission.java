package com.example.invoiceintelligence.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ClaimSubmission {
    private String id;
    private Instant createdAt;
    private List<UploadedDocument> documents;
    private List<DocumentClassification> classifications;
    private List<DocumentValidation> validations;
    private ClaimStatus status;
    private List<Discrepancy> discrepancies;
    private ClaimRecommendation recommendation;
}
