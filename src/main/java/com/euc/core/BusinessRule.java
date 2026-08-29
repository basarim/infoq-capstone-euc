package com.euc.core;

/**
 * A deterministic business requirement — something that is either satisfied
 * or not, independent of any model's judgment.
 *
 * A rule states what the business requires; an ExecutionRequirement states
 * the responsibility the application carries out to honour it. Keeping them
 * separate is what lets an EvaluationCriterion trace to the rule itself, not
 * merely to the step that happened to implement it.
 */
public class BusinessRule {

    private String id;
    private String description;

    public BusinessRule() {
        // default constructor for Jackson deserialization
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "BusinessRule{id='" + id + "', description='" + description + "'}";
    }
}
