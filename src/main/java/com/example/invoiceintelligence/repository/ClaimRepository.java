package com.example.invoiceintelligence.repository;

import com.example.invoiceintelligence.model.ClaimSubmission;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ClaimRepository {

    private final Map<String, ClaimSubmission> storage = new ConcurrentHashMap<>();

    public ClaimSubmission save(ClaimSubmission submission) {
        storage.put(submission.getId(), submission);
        return submission;
    }

    public Optional<ClaimSubmission> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}
