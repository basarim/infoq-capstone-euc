package com.euc.core;

import java.util.List;

/**
 * A single stage in the EUC's evaluation pipeline.
 *
 * Deliberately mirrors EucRule's shape (id + filter key) so execution and
 * evaluation are structurally parallel — the same schema pattern drives
 * both, per the project's central architectural claim. `evaluates` names
 * the execution pipeline stage IDs this evaluation stage checks, making
 * the link between "what ran" and "what got scored" traceable rather than
 * implicit.
 */
public class EvaluationStage {

    private String id;
    private String filter;
    private List<String> evaluates;

    public EvaluationStage() {
        // default constructor for Jackson deserialization
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public List<String> getEvaluates() {
        return evaluates;
    }

    public void setEvaluates(List<String> evaluates) {
        this.evaluates = evaluates;
    }

    @Override
    public String toString() {
        return "EvaluationStage{id='" + id + "', filter='" + filter + "', evaluates=" + evaluates + "}";
    }
}
