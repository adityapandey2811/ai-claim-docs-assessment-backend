package com.example.invoiceintelligence.ai;

import com.example.invoiceintelligence.model.DocumentClassification;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DocumentClassifierAssistant {

    @SystemMessage("""
            You are a document classification assistant for an insurance claims system.
            Classify the given document text into exactly one of these categories:
            CLAIM_FORM, POLICY_SCHEDULE, MEDICAL_BILL, UNKNOWN.
            Respond with the documentType, a confidence score between 0.0 and 1.0,
            and a short reasoning for your classification.
            """)
    @UserMessage("Document text:\n{{text}}")
    DocumentClassification classify(@V("text") String text);
}