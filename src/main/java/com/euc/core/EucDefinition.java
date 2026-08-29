package com.euc.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An Executable Use Case (EUC): a machine-readable statement of what the
 * business requires, that both the implementation and the evaluation point
 * back to.
 *
 * It says nothing about prompts, models, retrieval strategy, frameworks or
 * code — those are free to change, which is the point. What it does carry is
 * the link between the two halves: every EvaluationCriterion declares, via
 * `tracesTo`, which requirements, rules and policies it exists to validate.
 * See docs/proposal.md Section 3.
 */
public class EucDefinition {

    private String id;
    private String actor;
    private String goal;
    private EucContext context;
    private List<BusinessRule> rules;
    private List<Policy> policies;
    private List<String> expectedOutcomes;
    private List<ExecutionRequirement> executionRequirements;
    private List<EvaluationCriterion> evaluationCriteria;

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

    public EucContext getContext() {
        return context;
    }

    public void setContext(EucContext context) {
        this.context = context;
    }

    public List<BusinessRule> getRules() {
        return rules;
    }

    public void setRules(List<BusinessRule> rules) {
        this.rules = rules;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }

    public List<String> getExpectedOutcomes() {
        return expectedOutcomes;
    }

    public void setExpectedOutcomes(List<String> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
    }

    public List<ExecutionRequirement> getExecutionRequirements() {
        return executionRequirements;
    }

    public void setExecutionRequirements(List<ExecutionRequirement> executionRequirements) {
        this.executionRequirements = executionRequirements;
    }

    public List<EvaluationCriterion> getEvaluationCriteria() {
        return evaluationCriteria;
    }

    public void setEvaluationCriteria(List<EvaluationCriterion> evaluationCriteria) {
        this.evaluationCriteria = evaluationCriteria;
    }

    /** Requirements that are settled by an explicit check rather than model judgment. */
    public List<ExecutionRequirement> deterministicRequirements() {
        return executionRequirements.stream()
                .filter(r -> r.getType() == ExecutionRequirement.Type.DETERMINISTIC)
                .toList();
    }

    /** Requirements that need a model to weigh evidence and explain a judgment. */
    public List<ExecutionRequirement> reasonedRequirements() {
        return executionRequirements.stream()
                .filter(r -> r.getType() == ExecutionRequirement.Type.REASONED)
                .toList();
    }

    /** Looks up a single execution requirement by its id (e.g. "ELIGIBILITY-001"). */
    public ExecutionRequirement findRequirement(String requirementId) {
        return executionRequirements.stream()
                .filter(r -> r.getId().equals(requirementId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("No execution requirement with id " + requirementId));
    }

    /**
     * Every id a criterion is allowed to trace to: the execution
     * requirements, business rules and policies this EUC declares.
     */
    public Set<String> traceableIds() {
        Set<String> ids = new LinkedHashSet<>();
        if (executionRequirements != null) {
            executionRequirements.forEach(r -> ids.add(r.getId()));
        }
        if (rules != null) {
            rules.forEach(r -> ids.add(r.getId()));
        }
        if (policies != null) {
            policies.forEach(p -> ids.add(p.getId()));
        }
        return ids;
    }

    /**
     * Ids that no evaluation criterion traces to — the EUC's own account of
     * where traceability is incomplete.
     *
     * The proposal treats a requirement nobody checks as a finding worth
     * reporting rather than an error worth failing on (docs/proposal.md,
     * "Visible mapping gaps"): some requirements may legitimately be
     * unmeasurable, and hiding that would defeat the purpose. validate()
     * therefore does not reject these; it is for callers to report them.
     */
    public List<String> untracedIds() {
        Set<String> traced = new LinkedHashSet<>();
        if (evaluationCriteria != null) {
            for (EvaluationCriterion criterion : evaluationCriteria) {
                if (criterion.getTracesTo() != null) {
                    traced.addAll(criterion.getTracesTo());
                }
            }
        }
        List<String> gaps = new ArrayList<>(traceableIds());
        gaps.removeAll(traced);
        return gaps;
    }

    /**
     * Validates the EUC's structural contract at load time, so a malformed
     * definition fails loudly rather than silently producing an application
     * that does nothing or an evaluation that measures nothing.
     *
     * The check that matters most is the last one. A `tracesTo` entry naming
     * something the EUC does not declare is a broken link between evaluation
     * and business intent — precisely the failure this artifact exists to
     * prevent — so it is rejected here rather than discovered later as a
     * result nobody can explain.
     */
    public void validate() {
        if (executionRequirements == null || executionRequirements.isEmpty()) {
            throw new IllegalStateException(
                    "EUC '" + id + "' is invalid: executionRequirements must declare one or more responsibilities");
        }
        if (evaluationCriteria == null || evaluationCriteria.isEmpty()) {
            throw new IllegalStateException(
                    "EUC '" + id + "' is invalid: evaluationCriteria must declare one or more criteria");
        }
        for (ExecutionRequirement requirement : executionRequirements) {
            if (requirement.getId() == null || requirement.getId().isBlank()) {
                throw new IllegalStateException(
                        "EUC '" + id + "' is invalid: an execution requirement has no id");
            }
            if (requirement.getType() == null) {
                throw new IllegalStateException("EUC '" + id + "' is invalid: execution requirement '"
                        + requirement.getId() + "' has no type");
            }
        }

        Set<String> traceable = traceableIds();
        for (EvaluationCriterion criterion : evaluationCriteria) {
            if (criterion.getId() == null || criterion.getId().isBlank()) {
                throw new IllegalStateException(
                        "EUC '" + id + "' is invalid: an evaluation criterion has no id");
            }
            if (criterion.getTracesTo() == null || criterion.getTracesTo().isEmpty()) {
                throw new IllegalStateException("EUC '" + id + "' is invalid: evaluation criterion '"
                        + criterion.getId() + "' traces to nothing — a criterion that names no requirement"
                        + " cannot connect its result back to business intent");
            }
            for (String target : criterion.getTracesTo()) {
                if (!traceable.contains(target)) {
                    throw new IllegalStateException("EUC '" + id + "' is invalid: evaluation criterion '"
                            + criterion.getId() + "' traces to '" + target
                            + "', which is not a declared execution requirement, rule or policy");
                }
            }
        }
    }
}
