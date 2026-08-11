package com.euc.core;

import java.util.List;

/**
 * Declares the shared context that flows through the pipeline.
 *
 * In the Pipe-and-Filter pattern, filters don't exchange isolated
 * arguments — they read from and write to a shared context object that
 * accumulates state as it passes through the pipeline. `seedFields` names
 * what's present in that context before any filter runs (the pipeline's
 * initial inputs); each stage's `reads`/`writes` (see EucRule,
 * EvaluationStage) then declare how it consumes and grows that context.
 *
 * Evaluation stages read the same context execution stages wrote into,
 * rather than a separate copy — this is what makes the evaluation
 * pipeline's `reads` traceable to the execution pipeline's `writes`,
 * reinforcing the same-artifact-drives-both-execution-and-evaluation
 * claim (docs/proposal.md Section 1) at the data level, not just the
 * schema level.
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
