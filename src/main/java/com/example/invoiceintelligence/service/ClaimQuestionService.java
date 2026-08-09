package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.ai.ClaimQuestionAssistant;
import com.example.invoiceintelligence.model.ClaimSubmission;
import com.example.invoiceintelligence.model.UploadedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClaimQuestionService {

    private static final Logger log = LoggerFactory.getLogger(ClaimQuestionService.class);
    private final ClaimQuestionAssistant claimQuestionAssistant;

    public ClaimQuestionService(ClaimQuestionAssistant claimQuestionAssistant) {
        this.claimQuestionAssistant = claimQuestionAssistant;
    }

    public String answerQuestion(ClaimSubmission claimSubmission, String question) {
        log.info("Answering question for claim {}", claimSubmission.getId());

        String claimSummary = buildClaimSummary(claimSubmission);
        String answer = claimQuestionAssistant.answer(claimSummary, question);

        log.info("Claim question answered for claim {}", claimSubmission.getId());
        return answer;
    }

    private String buildClaimSummary(ClaimSubmission claim) {
        StringBuilder sb = new StringBuilder();
        sb.append("Claim ID: ").append(claim.getId()).append("\n");
        sb.append("Status: ").append(claim.getStatus()).append("\n\n");

        if (claim.getDocuments() != null) {
            for (UploadedDocument doc : claim.getDocuments()) {
                sb.append("--- Document: ").append(doc.getFileName()).append(" ---\n");
                sb.append(doc.getExtractedText()).append("\n\n");
            }
        }

        if (claim.getDiscrepancies() != null && !claim.getDiscrepancies().isEmpty()) {
            sb.append("Discrepancies found:\n");
            claim.getDiscrepancies().forEach(d ->
                    sb.append("- ").append(d.getField()).append(": ").append(d.getDescription()).append("\n"));
            sb.append("\n");
        }

        if (claim.getRecommendation() != null) {
            sb.append("Recommendation: ").append(claim.getRecommendation().getRecommendation()).append("\n");
            sb.append("Reasoning: ").append(claim.getRecommendation().getReasoning()).append("\n");
        }

        return sb.toString();
    }
}