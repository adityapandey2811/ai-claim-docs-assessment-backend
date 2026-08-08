package com.example.invoiceintelligence.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DocumentValidation {
    private String documentId;
    private boolean valid;
    private List<String> reasons;
}
