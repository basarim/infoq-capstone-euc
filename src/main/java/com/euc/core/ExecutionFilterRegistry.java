package com.euc.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps an EUC execution stage's `filter` key (e.g. "eligibility") to the
 * ExecutionFilter implementation that runs it. PipelineBuilder resolves
 * every stage through this registry rather than a hand-written switch, so
 * the executionPipeline's declared order and filter keys are what actually
 * drive execution.
 */
public class ExecutionFilterRegistry {

    private final Map<String, ExecutionFilter> filters = new HashMap<>();

    public ExecutionFilterRegistry register(String filterKey, ExecutionFilter filter) {
        filters.put(filterKey, filter);
        return this;
    }

    public ExecutionFilter get(String filterKey) {
        ExecutionFilter filter = filters.get(filterKey);
        if (filter == null) {
            throw new IllegalStateException(
                    "No ExecutionFilter registered for filter key '" + filterKey + "'");
        }
        return filter;
    }
}
