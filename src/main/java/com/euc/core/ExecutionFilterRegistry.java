package com.euc.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds each ExecutionRequirement id (e.g. "ELIGIBILITY-001") to the code
 * that satisfies it.
 *
 * This binding lives here, in the implementation, rather than in the EUC.
 * The business artifact declares what must happen; this registry is where a
 * particular implementation says "and this class is how we do it" — so
 * swapping the implementation never touches the statement of intent.
 */
public class ExecutionFilterRegistry {

    private final Map<String, ExecutionFilter> byRequirementId = new HashMap<>();

    public ExecutionFilterRegistry register(String requirementId, ExecutionFilter filter) {
        byRequirementId.put(requirementId, filter);
        return this;
    }

    public ExecutionFilter get(String requirementId) {
        ExecutionFilter filter = byRequirementId.get(requirementId);
        if (filter == null) {
            throw new IllegalStateException(
                    "No implementation registered for execution requirement '" + requirementId + "'");
        }
        return filter;
    }
}
