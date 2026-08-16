package com.example.invoiceintelligence.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ClaimQuestionAssistant {

    @SystemMessage("""
            You are a claims assistant answering follow-up questions about a specific
            insurance claim. Use only the claim details and any retrieved policy
            knowledge provided to you - do not invent information. Answer concisely.
            """)
    @UserMessage("""
            Claim details:
            {{claimSummary}}

            Question: {{question}}
            """)
    String answer(
            @MemoryId String claimId,
            @V("claimSummary") String claimSummary,
            @V("question") String question
    );
}