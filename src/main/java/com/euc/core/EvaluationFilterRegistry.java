package com.euc.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps an EUC evaluation stage's `filter` key (e.g. "eligibilityCorrectness")
 * to the EvaluationFilter implementation that scores it. Mirrors
 * ExecutionFilterRegistry for the evaluation half of the pipeline.
 */
public class EvaluationFilterRegistry {

    private final Map<String, EvaluationFilter> filters = new HashMap<>();

    public EvaluationFilterRegistry register(String filterKey, EvaluationFilter filter) {
        filters.put(filterKey, filter);
        return this;
    }

    public EvaluationFilter get(String filterKey) {
        EvaluationFilter filter = filters.get(filterKey);
        if (filter == null) {
            throw new IllegalStateException(
                    "No EvaluationFilter registered for filter key '" + filterKey + "'");
        }
        return filter;
    }
}
