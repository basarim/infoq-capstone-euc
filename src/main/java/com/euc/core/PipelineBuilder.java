package com.euc.core;

/**
 * Runs the EUC's execution requirements in declared order, resolving each
 * one to its implementation through an ExecutionFilterRegistry.
 *
 * There is no orchestration logic specific to any use case here: the EUC
 * says what must happen and in what order, the registry says what code
 * carries each step out, and this class does nothing but walk the list.
 *
 * A requirement whose outcome is FAILED and whose onFailure policy is HALT
 * stops the run immediately. That is the business contract being honoured —
 * "strong alignment cannot overcome a failed mandatory requirement" is
 * enforced here because the EUC says so, not because application code
 * happens to be written that way.
 */
public class PipelineBuilder {

    private final EucDefinition euc;
    private final ExecutionFilterRegistry registry;

    public PipelineBuilder(EucDefinition euc, ExecutionFilterRegistry registry) {
        this.euc = euc;
        this.registry = registry;
    }

    /** Carries out every execution requirement against the given context, in declared order. */
    public void run(PipelineContext context) {
        for (ExecutionRequirement requirement : euc.getExecutionRequirements()) {
            ExecutionFilter filter = registry.get(requirement.getId());
            ExecutionFilter.Outcome outcome = filter.execute(context, requirement);
            if (outcome == ExecutionFilter.Outcome.FAILED
                    && requirement.getOnFailure() == ExecutionRequirement.OnFailure.HALT) {
                break;
            }
        }
    }
}
