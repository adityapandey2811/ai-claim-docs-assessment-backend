package com.example.invoiceintelligence.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadedDocument {
    private String id;
    private String fileName;
    private String extractedText;
}
