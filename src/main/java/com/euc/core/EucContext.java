package com.euc.core;

import java.util.List;

/**
 * The business information this use case depends on.
 *
 * `seedFields` names what must already be present before any execution
 * requirement runs — the decision's inputs. Each requirement then declares
 * what it reads and what it writes, so the context grows as the use case
 * proceeds.
 *
 * Evaluation reads the same context execution wrote into, rather than a
 * separate copy. That is what makes an EvaluationCriterion's `reads`
 * genuinely connected to an ExecutionRequirement's `writes`: the two halves
 * are looking at the same data, not at two descriptions of it.
 */
public class EucContext {

    private String description;
    private List<String> seedFields;

    public EucContext() {
        // default constructor for Jackson deserialization
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSeedFields() {
        return seedFields;
    }

    public void setSeedFields(List<String> seedFields) {
        this.seedFields = seedFields;
    }

    @Override
    public String toString() {
        return "EucContext{seedFields=" + seedFields + ", description='" + description + "'}";
    }
}
