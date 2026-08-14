package com.euc.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assembles and runs the EUC's evaluationPipeline mechanically, mirroring
 * PipelineBuilder for the evaluation half: each stage's `filter` key is
 * resolved through an EvaluationFilterRegistry and run in declared order.
 *
 * Unlike execution stages, evaluation stages carry no onFailure policy —
 * every stage runs regardless of another stage's verdict, since each
 * evaluates an independent criterion rather than gating a shared outcome.
 * The caller is responsible for seeding the context with both the fields
 * execution wrote and whatever ground-truth fields the filters compare
 * against (see docs/proposal.md Section 4 on the shared-context pattern).
 */
public class EvaluationPipelineBuilder {

    private final EucDefinition euc;
    private final EvaluationFilterRegistry registry;

    public EvaluationPipelineBuilder(EucDefinition euc, EvaluationFilterRegistry registry) {
        this.euc = euc;
        this.registry = registry;
    }

    /** Runs every evaluation stage against the given context, returning each stage's verdict keyed by stage id. */
    public Map<String, EvaluationFilter.Verdict> run(PipelineContext context) {
        Map<String, EvaluationFilter.Verdict> verdicts = new LinkedHashMap<>();
        for (EvaluationStage stage : euc.getEvaluationPipeline()) {
            EvaluationFilter filter = registry.get(stage.getFilter());
            verdicts.put(stage.getId(), filter.evaluate(context, stage));
        }
        return verdicts;
    }
}
