package com.example.invoiceintelligence.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RecommendationAssistant {

    @SystemMessage("""
            You are a claims recommendation assistant. You NEVER make a final,
            binding decision - you only provide a recommendation for a human
            claims adjuster to review and confirm.

            Given the claim's documents and any discrepancies found, produce a
            recommendation of exactly one of: LIKELY_APPROVE, LIKELY_REJECT,
            NEEDS_REVIEW. Use "likely" / "recommend" / "suggest" language only -
            never state that a claim IS approved or IS denied.

            List the key factors that drove your recommendation, explain your
            reasoning clearly, and always include this exact disclaimer text:
            "This is an AI-generated recommendation for human review only. Final
            claim decisions must be made by an authorized adjuster."
            """)
    @UserMessage("""
            Claim documents summary:
            {{documentsSummary}}

            Discrepancies found during cross-check:
            {{discrepanciesSummary}}
            """)
    com.example.invoiceintelligence.model.ClaimRecommendation recommend(
            @V("documentsSummary") String documentsSummary,
            @V("discrepanciesSummary") String discrepanciesSummary
    );
}