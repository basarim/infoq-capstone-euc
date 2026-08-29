package com.euc.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds each EvaluationCriterion id (e.g. "EVAL-ELIGIBILITY") to the code
 * that scores it. Mirrors ExecutionFilterRegistry: the evaluator names the
 * criterion it measures, so the EUC never has to name an evaluator.
 */
public class EvaluationFilterRegistry {

    private final Map<String, EvaluationFilter> byCriterionId = new HashMap<>();

    public EvaluationFilterRegistry register(String criterionId, EvaluationFilter filter) {
        byCriterionId.put(criterionId, filter);
        return this;
    }

    public EvaluationFilter get(String criterionId) {
        EvaluationFilter filter = byCriterionId.get(criterionId);
        if (filter == null) {
            throw new IllegalStateException(
                    "No evaluator registered for criterion '" + criterionId + "'");
        }
        return filter;
    }
}
