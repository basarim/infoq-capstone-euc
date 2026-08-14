package com.euc.core;

/**
 * A single filter in the Pipe-and-Filter execution pipeline. Implementations
 * are looked up by PipelineBuilder via the `filter` key each EucRule stage
 * declares (an ExecutionFilterRegistry maps that key to an instance), so
 * adding a new stage to the EUC's executionPipeline means adding a filter
 * implementation and a registry entry — not editing the pipeline runner.
 */
public interface ExecutionFilter {

    enum Outcome {
        PASSED,
        FAILED
    }

    /**
     * Runs this stage against the shared context, reading whatever fields
     * it declares in `reads` and writing whatever it declares in `writes`
     * (see EucRule). Returning FAILED only stops the pipeline if the
     * stage's onFailure policy is HALT — PipelineBuilder makes that call,
     * not the filter itself.
     */
    Outcome execute(PipelineContext context, EucRule stage);
}
