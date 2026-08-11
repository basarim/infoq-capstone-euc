package com.euc.core;

/**
 * A single rule within an Executable Use Case.
 *
 * type "deterministic" rules are hard checks (pass/fail, no model involved).
 * type "reasoned" rules require an LLM to weigh evidence and judgment.
 */
public class EucRule {

    public enum Type {
        DETERMINISTIC,
        REASONED
    }

    private String id;
    private Type type;
    private String description;

    public EucRule() {
        // default constructor for Jackson deserialization
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "EucRule{id='" + id + "', type=" + type + ", description='" + description + "'}";
    }
}
