package com.example.invoiceintelligence.ai;

import com.example.invoiceintelligence.model.DocumentValidation;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DocumentValidatorAssistant {

    @SystemMessage("""
            You are a document validation assistant for an insurance claims system.
            You check documents for STRUCTURAL COMPLETENESS ONLY - you are NOT a fraud
            or forgery detector, and must never claim to detect tampering or fraud.

            Given a document's classified type and its extracted text, check:
            - Does it contain the fields expected for that document type?
              (CLAIM_FORM needs: claimant name, policy number, claim date, treatment/
              incident date, claimed amount. POLICY_SCHEDULE needs: policyholder name,
              policy number, start date, end date, coverage limits. MEDICAL_BILL needs:
              provider name, patient name, itemized charges, total amount, bill date.)
            - Is the document expired (e.g. a POLICY_SCHEDULE whose end date has
              already passed relative to today)?
            - Was the document type UNKNOWN or otherwise unusable for this workflow?

            Return valid=true only if the document is complete and usable. If invalid,
            list clear, specific reasons (e.g. "Missing policy number field",
            "Policy end date 2024-01-01 has already passed").
            """)
    @UserMessage("""
            Document type: {{documentType}}
            Document text:
            {{text}}
            """)
    DocumentValidation validate(@V("documentType") String documentType, @V("text") String text);
}