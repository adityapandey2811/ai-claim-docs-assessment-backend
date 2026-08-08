package com.example.invoiceintelligence.service;

import com.example.invoiceintelligence.client.GeminiClient;
import com.example.invoiceintelligence.model.ClaimSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClaimQuestionService {

    private static final Logger log = LoggerFactory.getLogger(ClaimQuestionService.class);
    private final GeminiClient geminiClient;

    public ClaimQuestionService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String answerQuestion(ClaimSubmission claimSubmission, String question) {
        log.info("Answering question for claim {}", claimSubmission.getId());
        String answer = geminiClient.answerClaimQuestion(claimSubmission, question);
        log.info("Claim question answered for claim {}", claimSubmission.getId());
        return answer;
    }
}
