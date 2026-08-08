package com.example.invoiceintelligence.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Discrepancy {
    private String field;
    private List<String> documentsInvolved;
    private String description;
    private String severity;
}
