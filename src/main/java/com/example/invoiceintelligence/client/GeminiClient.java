package com.example.invoiceintelligence.client;

import com.example.invoiceintelligence.config.AppConfig;
import com.example.invoiceintelligence.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    // Matches ```json ... ``` or plain ``` ... ``` fences that Gemini sometimes
    // wraps JSON responses in, despite prompt instructions not to.
    private static final Pattern CODE_FENCE_PATTERN =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppConfig appConfig;

    public GeminiClient(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.restTemplate = createRestTemplate(appConfig.getRequestTimeoutSeconds());
    }

    private RestTemplate createRestTemplate(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return new RestTemplate(factory);
    }

    public String answerClaimQuestion(ClaimSubmission claimSubmission, String question) {
        String prompt = buildClaimQuestionPrompt(claimSubmission, question);
        return sendPrompt(prompt, false);
    }

    public DocumentClassification classifyDocument(String text) {
        String prompt = buildClassificationPrompt(text);
        String responseText = sendPrompt(prompt, true);
        return parseClassificationResponse(responseText);
    }

    public DocumentValidation validateDocument(String text, DocumentClassification classification) {
        String prompt = buildValidationPrompt(text, classification);
        String responseText = sendPrompt(prompt, true);
        return parseValidationResponse(responseText);
    }

    public List<Discrepancy> crossCheckDocuments(
            List<UploadedDocument> documents,
            List<DocumentClassification> classifications,
            List<DocumentValidation> validations
    ) {
        String prompt = buildCrossCheckPrompt(documents, classifications, validations);
        String responseText = sendPrompt(prompt, true);
        return parseDiscrepanciesResponse(responseText);
    }

    public ClaimRecommendation recommendClaim(
            List<UploadedDocument> documents,
            List<DocumentClassification> classifications,
            List<DocumentValidation> validations,
            List<Discrepancy> discrepancies
    ) {
        String prompt = buildRecommendationPrompt(documents, classifications, validations, discrepancies);
        String responseText = sendPrompt(prompt, true);
        return parseRecommendationResponse(responseText);
    }

    private String sendPrompt(String prompt, boolean expectJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> generationConfig = expectJson
                ? Map.of("responseMimeType", "application/json")
                : Map.of();

        Map<String, Object> requestPayload = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", generationConfig
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                appConfig.getGeminiEndpoint() + "?key=" + appConfig.getGeminiApiKey(),
                request,
                JsonNode.class
        );

        JsonNode body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Gemini returned an empty body");
        }

        // Surface prompt-level blocks (e.g. safety filters on the input) explicitly,
        // rather than falling through to a confusing "no candidates" error.
        if (body.has("promptFeedback") && body.path("promptFeedback").has("blockReason")) {
            String reason = body.path("promptFeedback").path("blockReason").asText("UNKNOWN");
            throw new RuntimeException("Gemini blocked the prompt: " + reason);
        }

        if (!body.has("candidates") || !body.get("candidates").isArray() || body.get("candidates").isEmpty()) {
            throw new RuntimeException("Gemini returned no response candidates");
        }

        JsonNode candidate = body.get("candidates").get(0);

        String finishReason = candidate.path("finishReason").asText("");
        JsonNode parts = candidate.path("content").path("parts");

        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("Gemini response missing text parts (finishReason=" + finishReason + ")");
        }

        // Concatenate all text parts (Gemini can split output across multiple parts).
        StringBuilder combined = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                combined.append(part.path("text").asText());
            }
        }

        String text = combined.toString();
        if (text.isBlank()) {
            throw new RuntimeException("Gemini response text was empty (finishReason=" + finishReason + ")");
        }

        if ("MAX_TOKENS".equals(finishReason)) {
            log.warn("Gemini response was truncated by MAX_TOKENS; result may be incomplete JSON");
        }

        return text;
    }

    private String buildClassificationPrompt(String text) {
        return "Classify the following document into exactly one of these categories: CLAIM_FORM, POLICY_SCHEDULE, MEDICAL_BILL, UNKNOWN. " +
                "Return raw JSON only in the form { \"documentType\": \"...\", \"confidence\": 0.0-1.0, \"reasoning\": \"...\" }. " +
                "Be explicit that this is a structural classification task only and not a fraud or tamper detection task.\n\n" +
                "Document text:\n" + text;
    }

    private String buildValidationPrompt(String text, DocumentClassification classification) {
        return "You are performing structural validation only for the named insurance document type. " +
                "Do not perform any fraud, tamper, or authenticity analysis. " +
                "Validate whether the document contains the required fields for its type and whether it is structurally usable for an insurance claim workflow. " +
                "Return raw JSON only in the form { \"valid\": true/false, \"reasons\": [ ... ] }. " +
                "If the classification is UNKNOWN, return valid=false and explain that the document cannot be used.\n\n" +
                "Document type: " + classification.getDocumentType() + "\n" +
                "Classification confidence: " + classification.getConfidence() + "\n" +
                "Classification reasoning: " + classification.getReasoning() + "\n\n" +
                "Document text:\n" + text;
    }

    private String buildCrossCheckPrompt(
            List<UploadedDocument> documents,
            List<DocumentClassification> classifications,
            List<DocumentValidation> validations
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are a claims consistency checker. Review the structured document data and identify mismatches across documents. ");
        builder.append("Return raw JSON only as an array of discrepancies. Each discrepancy must include field, documentsInvolved, description, severity. ");
        builder.append("Do not make approval or denial decisions here.\n\n");

        for (UploadedDocument document : documents) {
            builder.append("Document ID: ").append(document.getId()).append("\n");
            builder.append("File name: ").append(document.getFileName()).append("\n");
            builder.append("Text:\n").append(document.getExtractedText()).append("\n\n");
        }

        builder.append("Classifications:\n");
        for (DocumentClassification classification : classifications) {
            builder.append(classification.getDocumentId()).append(": ")
                    .append(classification.getDocumentType()).append(" (confidence=")
                    .append(classification.getConfidence()).append(")\n");
        }
        builder.append("\nValidations:\n");
        for (DocumentValidation validation : validations) {
            builder.append(validation.getDocumentId()).append(": valid=")
                    .append(validation.isValid()).append(" reasons=")
                    .append(validation.getReasons()).append("\n");
        }

        builder.append("\nFocus on policy number consistency, coverage dates relative to treatment dates, and amounts consistency.\n");
        builder.append("If no discrepancies are found, return an empty array.\n");
        return builder.toString();
    }

    private String buildRecommendationPrompt(
            List<UploadedDocument> documents,
            List<DocumentClassification> classifications,
            List<DocumentValidation> validations,
            List<Discrepancy> discrepancies
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are an insurance claims recommendation assistant. Given the document classifications, validations, and consistency discrepancies, generate a recommendation in JSON only. ");
        builder.append("Do not use unconditional approval or rejection language. Always frame it as a likely recommendation and include a human-review disclaimer.\n\n");
        builder.append("Return JSON only with keys: recommendation, reasoning, keyFactors, disclaimer. ");
        builder.append("Recommendation must be one of LIKELY_APPROVE, LIKELY_REJECT, NEEDS_REVIEW.\n\n");

        builder.append("Documents:\n");
        for (UploadedDocument document : documents) {
            builder.append(document.getId()).append(": ").append(document.getFileName()).append("\n");
        }
        builder.append("\nClassifications:\n");
        for (DocumentClassification classification : classifications) {
            builder.append(classification.getDocumentId()).append(": ")
                    .append(classification.getDocumentType()).append(" (confidence=")
                    .append(classification.getConfidence()).append(")\n");
        }
        builder.append("\nValidations:\n");
        for (DocumentValidation validation : validations) {
            builder.append(validation.getDocumentId()).append(": valid=")
                    .append(validation.isValid()).append(" reasons=")
                    .append(validation.getReasons()).append("\n");
        }
        builder.append("\nDiscrepancies:\n").append(discrepancies).append("\n");
        builder.append("\nRecommendation note: This is a suggestion for a human adjuster and not a final decision.\n");
        return builder.toString();
    }

    private String buildClaimQuestionPrompt(ClaimSubmission claimSubmission, String question) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are an insurance claims assistant. Answer the user's question using the claim submission's documents, classifications, validations, discrepancies, and recommendation. ");
        builder.append("Do not provide a final decision. Frame the answer as guidance for a human adjuster only.\n\n");
        builder.append("Claim ID: ").append(claimSubmission.getId()).append("\n");
        builder.append("Created at: ").append(claimSubmission.getCreatedAt()).append("\n\n");

        builder.append("Documents:\n");
        claimSubmission.getDocuments().forEach(document -> builder.append(document.getId()).append(": ").append(document.getFileName()).append("\n"));
        builder.append("\n");

        builder.append("Classifications:\n");
        if (claimSubmission.getClassifications() != null) {
            claimSubmission.getClassifications().forEach(classification -> builder.append(classification.getDocumentId())
                    .append(": ").append(classification.getDocumentType())
                    .append(" (confidence=").append(classification.getConfidence()).append(")\n"));
        }
        builder.append("\nValidations:\n");
        if (claimSubmission.getValidations() != null) {
            claimSubmission.getValidations().forEach(validation -> builder.append(validation.getDocumentId())
                    .append(": valid=").append(validation.isValid()).append(" reasons=")
                    .append(validation.getReasons()).append("\n"));
        }

        builder.append("\nDiscrepancies:\n");
        if (claimSubmission.getDiscrepancies() != null) {
            builder.append(claimSubmission.getDiscrepancies()).append("\n");
        }

        builder.append("\nRecommendation:\n");
        if (claimSubmission.getRecommendation() != null) {
            builder.append(claimSubmission.getRecommendation()).append("\n");
        }

        builder.append("\nQuestion: ").append(question);
        return builder.toString();
    }

    private DocumentClassification parseClassificationResponse(String responseText) {
        String jsonText = extractJson(responseText);
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            return DocumentClassification.builder()
                    .documentType(textNode(root, "documentType"))
                    .confidence(root.path("confidence").asDouble(0.0))
                    .reasoning(textNode(root, "reasoning"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini classification response. Raw text was: {}", responseText);
            throw new RuntimeException("Failed to parse Gemini classification response", e);
        }
    }

    private DocumentValidation parseValidationResponse(String responseText) {
        String jsonText = extractJson(responseText);
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            return DocumentValidation.builder()
                    .valid(root.path("valid").asBoolean(false))
                    .reasons(listNode(root, "reasons"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini validation response. Raw text was: {}", responseText);
            throw new RuntimeException("Failed to parse Gemini validation response", e);
        }
    }

    private List<Discrepancy> parseDiscrepanciesResponse(String responseText) {
        String jsonText = extractJson(responseText);
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            List<Discrepancy> discrepancies = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode item : root) {
                    discrepancies.add(Discrepancy.builder()
                            .field(textNode(item, "field"))
                            .documentsInvolved(listNode(item, "documentsInvolved"))
                            .description(textNode(item, "description"))
                            .severity(textNode(item, "severity"))
                            .build());
                }
            }
            return discrepancies;
        } catch (Exception e) {
            log.error("Failed to parse Gemini discrepancy response. Raw text was: {}", responseText);
            throw new RuntimeException("Failed to parse Gemini discrepancy response", e);
        }
    }

    private ClaimRecommendation parseRecommendationResponse(String responseText) {
        String jsonText = extractJson(responseText);
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            return ClaimRecommendation.builder()
                    .recommendation(textNode(root, "recommendation"))
                    .reasoning(textNode(root, "reasoning"))
                    .keyFactors(listNode(root, "keyFactors"))
                    .disclaimer(textNode(root, "disclaimer"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini recommendation response. Raw text was: {}", responseText);
            throw new RuntimeException("Failed to parse Gemini recommendation response", e);
        }
    }

    /**
     * Gemini often wraps JSON in ```json ... ``` fences or adds leading/trailing
     * prose despite prompt instructions. This strips fences and, failing that,
     * extracts the outermost {...} block so parsing doesn't blow up on
     * otherwise-valid JSON.
     */
    private String extractJson(String responseText) {
        String trimmed = responseText.trim();

        Matcher fenceMatcher = CODE_FENCE_PATTERN.matcher(trimmed);
        if (fenceMatcher.find()) {
            return fenceMatcher.group(1).trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    private String textNode(JsonNode root, String name) {
        JsonNode node = root.path(name);
        return node.isTextual() ? node.asText() : "";
    }

    private List<String> listNode(JsonNode root, String name) {
        JsonNode node = root.path(name);
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> values.add(item.asText("")));
        }
        return values;
    }
}