package com.euc.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The shared context that flows through a PipelineBuilder run (see
 * EucContext for the schema-level declaration this mirrors at runtime).
 * Filters read fields written by earlier stages and write their own —
 * there is one context per pipeline run, not a copy per stage.
 */
public class PipelineContext {

    private final Map<String, Object> fields = new HashMap<>();

    public void put(String field, Object value) {
        fields.put(field, value);
    }

    public <T> T get(String field, Class<T> type) {
        Object value = fields.get(field);
        if (value == null) {
            throw new IllegalStateException(
                    "Context field '" + field + "' was not set by any stage that ran before it was read");
        }
        return type.cast(value);
    }

    public boolean has(String field) {
        return fields.containsKey(field);
    }

    @Override
    public String toString() {
        return "PipelineContext" + fields;
    }
}
