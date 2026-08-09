package com.example.invoiceintelligence.ai;

import com.example.invoiceintelligence.model.Discrepancy;

import java.util.List;

/**
 * Wrapper around List<Discrepancy> - LangChain4j structured output works most
 * reliably returning a single top-level object rather than a bare List<T>.
 */
public class DiscrepancyReport {
    private List<Discrepancy> discrepancies;

    public List<Discrepancy> getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(List<Discrepancy> discrepancies) {
        this.discrepancies = discrepancies;
    }
}