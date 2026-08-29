package com.euc.core;

import java.util.List;

/**
 * A business responsibility the application must carry out.
 *
 * This describes *what* must happen, not how. There is deliberately no
 * binding here to a class, a prompt, or a pipeline position: an
 * implementation registers a handler against this requirement's id (see
 * ExecutionFilterRegistry), so the implementation names the requirement it
 * satisfies rather than the requirement naming its implementation.
 *
 * `type` distinguishes a deterministic check from one that needs a model to
 * weigh evidence. `onFailure` makes short-circuit behavior an explicit part
 * of the business contract instead of an assumption buried in application
 * code — a failed mandatory requirement halting the run is a decision the
 * business made, not an implementation detail.
 */
public class ExecutionRequirement {

    public enum Type {
        DETERMINISTIC,
        REASONED
    }

    public enum OnFailure {
        HALT,
        CONTINUE
    }

    private String id;
    private Type type;
    private String responsibility;
    private OnFailure onFailure = OnFailure.CONTINUE;
    private List<String> reads;
    private List<String> writes;

    public ExecutionRequirement() {
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

    /** What the business requires this step to do, in business terms. */
    public String getResponsibility() {
        return responsibility;
    }

    public void setResponsibility(String responsibility) {
        this.responsibility = responsibility;
    }

    public OnFailure getOnFailure() {
        return onFailure;
    }

    public void setOnFailure(OnFailure onFailure) {
        this.onFailure = onFailure;
    }

    /**
     * Context fields this requirement depends on. Every field named here
     * should already be present — either from EucContext.seedFields or from
     * the `writes` of an earlier requirement.
     */
    public List<String> getReads() {
        return reads;
    }

    public void setReads(List<String> reads) {
        this.reads = reads;
    }

    /** Context fields this requirement adds to, or updates in, the shared context. */
    public List<String> getWrites() {
        return writes;
    }

    public void setWrites(List<String> writes) {
        this.writes = writes;
    }

    @Override
    public String toString() {
        return "ExecutionRequirement{id='" + id + "', type=" + type
                + ", onFailure=" + onFailure + ", reads=" + reads + ", writes=" + writes
                + ", responsibility='" + responsibility + "'}";
    }
}
