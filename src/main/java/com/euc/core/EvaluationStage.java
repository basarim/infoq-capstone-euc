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
    private Integer group;
    private List<String> reads;

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

    /**
     * Execution group, mirroring EucRule.getGroup() — see that javadoc.
     * Evaluation stages checking independent criteria are natural
     * candidates for future concurrent evaluation.
     */
    public Integer getGroup() {
        return group;
    }

    public void setGroup(Integer group) {
        this.group = group;
    }

    /**
     * Context fields this evaluation stage reads — typically fields the
     * execution pipeline wrote (see EucRule.getWrites()). Evaluation reads
     * the same context execution wrote into, not a separate copy — see
     * EucContext.
     */
    public List<String> getReads() {
        return reads;
    }

    public void setReads(List<String> reads) {
        this.reads = reads;
    }

    @Override
    public String toString() {
        return "EvaluationStage{id='" + id + "', filter='" + filter + "', evaluates=" + evaluates
                + ", group=" + group + ", reads=" + reads + "}";
    }
}
