package com.euc.core;

import java.util.List;

/**
 * An Executable Use Case (EUC): the single, machine-readable definition of
 * business intent that drives both application execution and evaluation.
 *
 * The structure is aligned with the Pipe-and-Filter pattern: executionPipeline
 * is an ordered list of filter stages (see EucRule) that a PipelineBuilder
 * can assemble mechanically, and evaluationPipeline is a structurally
 * parallel list of evaluation stages (see EvaluationStage) that reference
 * which execution stages they check. See docs/proposal.md, Section 3, for
 * the design rationale.
 */
public class EucDefinition {

    private String id;
    private String actor;
    private String goal;
    private List<EucRule> executionPipeline;
    private List<String> policies;
    private List<String> expectedOutcomes;
    private List<EvaluationStage> evaluationPipeline;

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

    public List<EucRule> getExecutionPipeline() {
        return executionPipeline;
    }

    public void setExecutionPipeline(List<EucRule> executionPipeline) {
        this.executionPipeline = executionPipeline;
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

    public List<EvaluationStage> getEvaluationPipeline() {
        return evaluationPipeline;
    }

    public void setEvaluationPipeline(List<EvaluationStage> evaluationPipeline) {
        this.evaluationPipeline = evaluationPipeline;
    }

    /** Convenience accessor: deterministic-only execution stages. */
    public List<EucRule> deterministicStages() {
        return executionPipeline.stream()
                .filter(r -> r.getType() == EucRule.Type.DETERMINISTIC)
                .toList();
    }

    /** Convenience accessor: reasoned (LLM-judged) execution stages. */
    public List<EucRule> reasonedStages() {
        return executionPipeline.stream()
                .filter(r -> r.getType() == EucRule.Type.REASONED)
                .toList();
    }

    /** Looks up a single execution stage by its id (e.g. "ELIGIBILITY-001"). */
    public EucRule findStage(String stageId) {
        return executionPipeline.stream()
                .filter(r -> r.getId().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No execution stage with id " + stageId));
    }
}
