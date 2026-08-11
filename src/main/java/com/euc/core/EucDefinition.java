package com.euc.core;

import java.util.List;

/**
 * An Executable Use Case (EUC): the single, machine-readable definition of
 * business intent that drives both application execution and evaluation.
 *
 * See docs/proposal.md, Section 3 ("Proposed Approach: Executable Use Cases")
 * for the design rationale behind this structure.
 */
public class EucDefinition {

    private String id;
    private String actor;
    private String goal;
    private List<EucRule> rules;
    private List<String> policies;
    private List<String> expectedOutcomes;
    private List<String> evaluation;

    public EucDefinition() {
        // default constructor for Jackson deserialization
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public List<EucRule> getRules() {
        return rules;
    }

    public void setRules(List<EucRule> rules) {
        this.rules = rules;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
    }

    public List<String> getExpectedOutcomes() {
        return expectedOutcomes;
    }

    public void setExpectedOutcomes(List<String> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
    }

    public List<String> getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(List<String> evaluation) {
        this.evaluation = evaluation;
    }

    /** Convenience accessor: deterministic-only rules. */
    public List<EucRule> deterministicRules() {
        return rules.stream()
                .filter(r -> r.getType() == EucRule.Type.DETERMINISTIC)
                .toList();
    }

    /** Convenience accessor: reasoned (LLM-judged) rules. */
    public List<EucRule> reasonedRules() {
        return rules.stream()
                .filter(r -> r.getType() == EucRule.Type.REASONED)
                .toList();
    }
}
