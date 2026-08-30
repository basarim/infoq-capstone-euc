package com.euc.core;

/**
 * A behavioral constraint that applies across the whole use case rather than
 * to any single step — "do not invent missing information" is not a property
 * of the alignment step, it is a constraint every reasoned step must respect.
 *
 * Structurally this plays the role of an aspect: it cuts across the execution
 * requirements instead of belonging to one of them. Policies carry an id so
 * an EvaluationCriterion can trace to the constraint it enforces.
 */
public class Policy {

    private String id;
    private String description;

    public Policy() {
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
        return "Policy{id='" + id + "', description='" + description + "'}";
    }
}
