package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.exception.ClaimNotFoundException;
import com.example.invoiceintelligence.model.*;
import com.example.invoiceintelligence.repository.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClaimProcessingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ClaimProcessingOrchestrator.class);

    private final OcrService ocrService;
    private final DocumentClassifierService classifierService;
    private final DocumentValidatorService validatorService;
    private final CrossCheckService crossCheckService;
    private final RecommendationService recommendationService;
    private final ClaimRepository claimRepository;

    public ClaimProcessingOrchestrator(
            OcrService ocrService,
            DocumentClassifierService classifierService,
            DocumentValidatorService validatorService,
            CrossCheckService crossCheckService,
            RecommendationService recommendationService,
            ClaimRepository claimRepository
    ) {
        this.ocrService = ocrService;
        this.classifierService = classifierService;
        this.validatorService = validatorService;
        this.crossCheckService = crossCheckService;
        this.recommendationService = recommendationService;
        this.claimRepository = claimRepository;
    }

    public ClaimSubmission submitClaim(List<MultipartFile> files) {
        log.info("Starting claim submission pipeline for {} files", files.size());
        List<UploadedDocument> uploadedDocuments = new ArrayList<>();
        for (MultipartFile file : files) {
            String extractedText = ocrService.extractText(file);
            UploadedDocument document = UploadedDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .fileName(file.getOriginalFilename())
                    .extractedText(extractedText)
                    .build();
            uploadedDocuments.add(document);
        }

        ClaimSubmission submission = ClaimSubmission.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .documents(uploadedDocuments)
                .status(ClaimStatus.PROCESSING)
                .build();
        claimRepository.save(submission);

        List<DocumentClassification> classifications = classifierService.classifyDocuments(uploadedDocuments);
        submission.setClassifications(classifications);
        claimRepository.save(submission);

        List<DocumentValidation> validations = validatorService.validateDocuments(uploadedDocuments, classifications);
        submission.setValidations(validations);

        boolean anyInvalid = validations.stream().anyMatch(validation -> !validation.isValid());
        if (anyInvalid) {
            submission.setStatus(ClaimStatus.REJECTED_INVALID_DOCS);
            claimRepository.save(submission);
            log.warn("Claim {} rejected due to invalid document(s)", submission.getId());
            return submission;
        }

        claimRepository.save(submission);

        List<Discrepancy> discrepancies = crossCheckService.checkConsistency(uploadedDocuments, classifications, validations);
        submission.setDiscrepancies(discrepancies);
        claimRepository.save(submission);

        ClaimRecommendation recommendation = recommendationService.recommend(uploadedDocuments, classifications, validations, discrepancies);
        submission.setRecommendation(recommendation);
        submission.setStatus(ClaimStatus.AWAITING_REVIEW);
        claimRepository.save(submission);

        log.info("Claim {} pipeline completed with status {}", submission.getId(), submission.getStatus());
        return submission;
    }

    public ClaimSubmission getClaim(String id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + id));
    }
}
