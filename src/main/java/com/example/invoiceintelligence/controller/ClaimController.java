package com.example.invoiceintelligence.controller;

import com.example.invoiceintelligence.model.ClaimSubmission;
import com.example.invoiceintelligence.model.QuestionRequest;
import com.example.invoiceintelligence.model.QuestionResponse;
import com.example.invoiceintelligence.service.ClaimProcessingOrchestrator;
import com.example.invoiceintelligence.service.ClaimQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimProcessingOrchestrator orchestrator;
    private final ClaimQuestionService claimQuestionService;

    public ClaimController(ClaimProcessingOrchestrator orchestrator, ClaimQuestionService claimQuestionService) {
        this.orchestrator = orchestrator;
        this.claimQuestionService = claimQuestionService;
    }

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClaimSubmission> submitClaim(@RequestParam("files") List<MultipartFile> files) {
        ClaimSubmission submission = orchestrator.submitClaim(files);
        return ResponseEntity.status(HttpStatus.CREATED).body(submission);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimSubmission> getClaim(@PathVariable("id") String id) {
        ClaimSubmission submission = orchestrator.getClaim(id);
        return ResponseEntity.ok(submission);
    }

    @PostMapping("/{id}/query")
    public ResponseEntity<QuestionResponse> askClaimQuestion(
            @PathVariable("id") String id,
            @Valid @RequestBody QuestionRequest request
    ) {
        ClaimSubmission submission = orchestrator.getClaim(id);
        String answer = claimQuestionService.answerQuestion(submission, request.getQuestion());
        return ResponseEntity.ok(new QuestionResponse(answer));
    }
}
