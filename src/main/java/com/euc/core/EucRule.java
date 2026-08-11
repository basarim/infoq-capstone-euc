package com.euc.core;

/**
 * A single stage in the EUC's execution pipeline.
 *
 * Deliberately shaped around the Pipe-and-Filter pattern: each stage names
 * a `filter` (a lookup key into a filter registry, so a PipelineBuilder can
 * assemble the execution chain mechanically from the EUC rather than a
 * human hand-wiring it per use case), a `type` (deterministic = hard
 * pass/fail check, reasoned = requires an LLM to weigh evidence), and an
 * `onFailure` policy that makes short-circuit behavior an explicit schema
 * contract instead of an assumption buried in application code.
 */
public class EucRule {

    public enum Type {
        DETERMINISTIC,
        REASONED
    }

    public enum OnFailure {
        HALT,
        CONTINUE
    }

    private String id;
    private String filter;
    private Type type;
    private String description;
    private OnFailure onFailure = OnFailure.CONTINUE;
    private Integer group;

    public EucRule() {
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

    public OnFailure getOnFailure() {
        return onFailure;
    }

    public void setOnFailure(OnFailure onFailure) {
        this.onFailure = onFailure;
    }

    /**
     * Execution group. Stages execute in ascending group order; stages
     * sharing the same group number are candidates for concurrent
     * execution by a future engine. This version's PipelineBuilder (not
     * yet implemented) is expected to execute strictly in array order
     * regardless of group — every stage in the current EUC has a unique
     * group number, so behavior stays fully serialized. The field exists
     * so a future concurrent execution model doesn't require a schema
     * migration.
     */
    public Integer getGroup() {
        return group;
    }

    public void setGroup(Integer group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "EucRule{id='" + id + "', filter='" + filter + "', type=" + type
                + ", onFailure=" + onFailure + ", group=" + group + ", description='" + description + "'}";
    }
}
