package com.euc.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests PipelineBuilder in isolation, independent of any specific use case
 * (Grant Fit Assessment exercises the same contract indirectly through
 * GrantFitApplicationTest, but this asserts the ordering and halt behaviour
 * directly against the LangGraph4j-backed engine).
 */
class PipelineBuilderTest {

    @Test
    void requirementsRunInDeclaredOrder() {
        List<String> invoked = new ArrayList<>();
        EucDefinition euc = eucWith(
                requirement("STEP-A", ExecutionRequirement.OnFailure.HALT),
                requirement("STEP-B", ExecutionRequirement.OnFailure.HALT),
                requirement("STEP-C", ExecutionRequirement.OnFailure.HALT)
        );
        ExecutionFilterRegistry registry = new ExecutionFilterRegistry()
                .register("STEP-A", recordingFilter(invoked, ExecutionFilter.Outcome.PASSED))
                .register("STEP-B", recordingFilter(invoked, ExecutionFilter.Outcome.PASSED))
                .register("STEP-C", recordingFilter(invoked, ExecutionFilter.Outcome.PASSED));

        new PipelineBuilder(euc, registry).run(new PipelineContext());

        assertEquals(List.of("STEP-A", "STEP-B", "STEP-C"), invoked);
    }

    @Test
    void failedRequirementWithHaltStopsTheRunBeforeTheNextOne() {
        List<String> invoked = new ArrayList<>();
        EucDefinition euc = eucWith(
                requirement("STEP-A", ExecutionRequirement.OnFailure.HALT),
                requirement("STEP-B", ExecutionRequirement.OnFailure.HALT),
                requirement("STEP-C", ExecutionRequirement.OnFailure.HALT)
        );
        ExecutionFilterRegistry registry = new ExecutionFilterRegistry()
                .register("STEP-A", recordingFilter(invoked, ExecutionFilter.Outcome.PASSED))
                .register("STEP-B", recordingFilter(invoked, ExecutionFilter.Outcome.FAILED))
                .register("STEP-C", recordingFilter(invoked, ExecutionFilter.Outcome.PASSED));

        new PipelineBuilder(euc, registry).run(new PipelineContext());

        assertEquals(List.of("STEP-A", "STEP-B"), invoked, "STEP-C must not run once STEP-B halts the pipeline");
    }

    @Test
    void failedRequirementWithContinueDoesNotStopTheRun() {
        List<String> invoked = new ArrayList<>();
        EucDefinition euc = eucWith(
                requirement("STEP-A", ExecutionRequirement.OnFailure.HALT),
                requirement("STEP-B", ExecutionRequirement.OnFailure.CONTINUE),
                requirement("STEP-C", ExecutionRequirement.OnFailure.HALT)
        );
        ExecutionFilterRegistry registry = new ExecutionFilterRegistry()
                .register("STEP-A", recordingFilter(invoked, ExecutionFilter.Outcome.PASSED))
                .register("STEP-B", recordingFilter(invoked, ExecutionFilter.Outcome.FAILED))
                .register("STEP-C", recordingFilter(invoked, ExecutionFilter.Outcome.PASSED));

        new PipelineBuilder(euc, registry).run(new PipelineContext());

        assertEquals(List.of("STEP-A", "STEP-B", "STEP-C"), invoked,
                "onFailure: continue must not halt the pipeline");
    }

    @Test
    void filtersReadAndWriteTheSamePipelineContextPassedToRun() {
        EucDefinition euc = eucWith(requirement("STEP-A", ExecutionRequirement.OnFailure.HALT));
        ExecutionFilterRegistry registry = new ExecutionFilterRegistry()
                .register("STEP-A", (context, stage) -> {
                    context.put("marker", "written-by-STEP-A");
                    return ExecutionFilter.Outcome.PASSED;
                });
        PipelineContext context = new PipelineContext();

        new PipelineBuilder(euc, registry).run(context);

        assertEquals("written-by-STEP-A", context.get("marker", String.class));
    }

    private static EucDefinition eucWith(ExecutionRequirement... requirements) {
        EucDefinition euc = new EucDefinition();
        euc.setId("pipeline-builder-test");
        euc.setExecutionRequirements(List.of(requirements));
        return euc;
    }

    private static ExecutionRequirement requirement(String id, ExecutionRequirement.OnFailure onFailure) {
        ExecutionRequirement requirement = new ExecutionRequirement();
        requirement.setId(id);
        requirement.setType(ExecutionRequirement.Type.DETERMINISTIC);
        requirement.setOnFailure(onFailure);
        return requirement;
    }

    private static ExecutionFilter recordingFilter(List<String> invoked, ExecutionFilter.Outcome outcome) {
        return (context, stage) -> {
            invoked.add(stage.getId());
            return outcome;
        };
    }
}
