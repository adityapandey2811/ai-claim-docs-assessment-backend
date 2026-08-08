package com.example.invoiceintelligence.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClaimRecommendation {
    private String recommendation;
    private String reasoning;
    private List<String> keyFactors;
    private String disclaimer;
}
