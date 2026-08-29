package com.euc.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scores every EvaluationCriterion the EUC declares, resolving each one to
 * its evaluator through an EvaluationFilterRegistry. Mirrors PipelineBuilder
 * for the evaluation half.
 *
 * Unlike execution requirements, criteria carry no onFailure policy — every
 * criterion is scored regardless of another's verdict, since each measures
 * something independent rather than gating a shared outcome. The caller
 * seeds the context with both what execution wrote and whatever ground truth
 * the criteria are compared against.
 *
 * Verdicts come back keyed by criterion id, which is what makes a result
 * traceable: from a verdict you have the criterion, and from the criterion's
 * `tracesTo` you have the business requirements it speaks to.
 */
public class EvaluationPipelineBuilder {

    private final EucDefinition euc;
    private final EvaluationFilterRegistry registry;

    public EvaluationPipelineBuilder(EucDefinition euc, EvaluationFilterRegistry registry) {
        this.euc = euc;
        this.registry = registry;
    }

    /** Scores every criterion against the given context, returning each verdict keyed by criterion id. */
    public Map<String, EvaluationFilter.Verdict> run(PipelineContext context) {
        Map<String, EvaluationFilter.Verdict> verdicts = new LinkedHashMap<>();
        for (EvaluationCriterion criterion : euc.getEvaluationCriteria()) {
            EvaluationFilter filter = registry.get(criterion.getId());
            verdicts.put(criterion.getId(), filter.evaluate(context, criterion));
        }
        return verdicts;
    }
}
