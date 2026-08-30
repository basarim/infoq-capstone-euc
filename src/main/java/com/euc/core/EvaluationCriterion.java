package com.euc.core;

import java.util.List;

/**
 * What must be checked, and which business requirement each check stands for.
 *
 * `tracesTo` is the keystone of the whole artifact. Every criterion names the
 * execution requirements, rules and policies it exists to validate, so an
 * evaluation result can be followed backwards to the business requirement it
 * came from. EucDefinition.validate() rejects a trace that does not resolve,
 * which makes a broken link a load-time failure rather than a silent gap.
 *
 * Note what is absent: no prompt, no model, no test file, no framework. A
 * criterion points at intent, which is exactly what lets the implementation
 * underneath be replaced without the evaluation losing its meaning.
 */
public class EvaluationCriterion {

    private String id;
    private List<String> tracesTo;
    private List<String> criteria;
    private List<String> reads;

    public EvaluationCriterion() {
        // default constructor for Jackson deserialization
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Ids of the execution requirements, business rules and policies this
     * criterion validates. Each must resolve within the same EUC.
     */
    public List<String> getTracesTo() {
        return tracesTo;
    }

    public void setTracesTo(List<String> tracesTo) {
        this.tracesTo = tracesTo;
    }

    /** The checks themselves, stated in business terms. */
    public List<String> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<String> criteria) {
        this.criteria = criteria;
    }

    /**
     * Context fields this criterion reads — typically fields an execution
     * requirement wrote. Evaluation reads the same context execution wrote
     * into, not a separate copy.
     */
    public List<String> getReads() {
        return reads;
    }

    public void setReads(List<String> reads) {
        this.reads = reads;
    }

    @Override
    public String toString() {
        return "EvaluationCriterion{id='" + id + "', tracesTo=" + tracesTo
                + ", criteria=" + criteria + ", reads=" + reads + "}";
    }
}
