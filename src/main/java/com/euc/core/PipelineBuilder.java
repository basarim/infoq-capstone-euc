package com.euc.core;

/**
 * Assembles and runs the EUC's executionPipeline mechanically: each stage's
 * `filter` key is resolved through an ExecutionFilterRegistry and executed
 * in the pipeline's declared array order.
 *
 * This version executes strictly in array order regardless of `group` —
 * see EucRule.getGroup() javadoc. Every stage in the current EUC has a
 * unique group number, so array order and group order coincide and
 * behavior stays fully serial; a future concurrent engine could instead
 * run same-group stages together without a schema change.
 *
 * A stage whose outcome is FAILED and whose onFailure policy is HALT stops
 * the pipeline immediately — no later stage runs, matching the EUC's
 * explicit onFailure contract (docs/proposal.md Section 4) rather than an
 * assumption buried in application code.
 */
public class PipelineBuilder {

    private final EucDefinition euc;
    private final ExecutionFilterRegistry registry;

    public PipelineBuilder(EucDefinition euc, ExecutionFilterRegistry registry) {
        this.euc = euc;
        this.registry = registry;
    }

    /** Runs every execution stage against the given context, in declared order. */
    public void run(PipelineContext context) {
        for (EucRule stage : euc.getExecutionPipeline()) {
            ExecutionFilter filter = registry.get(stage.getFilter());
            ExecutionFilter.Outcome outcome = filter.execute(context, stage);
            if (outcome == ExecutionFilter.Outcome.FAILED && stage.getOnFailure() == EucRule.OnFailure.HALT) {
                break;
            }
        }
    }
}
