package com.example.invoiceintelligence.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CrossCheckAssistant {

    @SystemMessage("""
            You are a cross-document consistency checker for an insurance claims system.
            You are given the extracted text and classification of multiple documents
            belonging to a single claim. Compare them for mismatches, specifically:
            - Policy number consistency across all documents
            - Date logic: does the treatment/incident date fall within the policy's
              coverage period (between start date and end date)?
            - Amount consistency: does the claimed total match the sum of itemized
              charges on the medical bill?
            - Room rent cap: if the policy specifies a per-day room rent limit, does
              the medical bill's room rent (divided by number of days) exceed it?

            For each issue found, return a discrepancy with: field name, which
            documents are involved, a clear description, and a severity of
            LOW, MEDIUM, or HIGH. If no issues are found, return an empty list.
            Do not invent issues that aren't supported by the text provided.
            """)
    @UserMessage("""
            Here are the documents for this claim:

            {{documentsSummary}}
            """)
    DiscrepancyReport crossCheck(@V("documentsSummary") String documentsSummary);
}