"""Tests PipelineBuilder in isolation, independent of any specific use case
(Grant Fit Assessment exercises the same contract indirectly through
test_grant_fit_application.py, but this asserts the ordering and halt
behaviour directly against the LangGraph-backed engine)."""

from __future__ import annotations

from euc.core.context import PipelineContext
from euc.core.filters import ExecutionFilterRegistry, Outcome
from euc.core.models import EucDefinition, ExecutionRequirement, ExecutionRequirementType, OnFailure
from euc.core.pipeline import PipelineBuilder


def _euc_with(*requirements: ExecutionRequirement) -> EucDefinition:
    return EucDefinition(id="pipeline-builder-test", execution_requirements=list(requirements))


def _requirement(id_: str, on_failure: OnFailure) -> ExecutionRequirement:
    return ExecutionRequirement(id=id_, type=ExecutionRequirementType.DETERMINISTIC, on_failure=on_failure)


def _recording_filter(invoked: list[str], outcome: Outcome):
    def filter_fn(context: PipelineContext, stage: ExecutionRequirement) -> Outcome:
        invoked.append(stage.id)
        return outcome

    return filter_fn


def test_requirements_run_in_declared_order():
    invoked: list[str] = []
    euc = _euc_with(
        _requirement("STEP-A", OnFailure.HALT),
        _requirement("STEP-B", OnFailure.HALT),
        _requirement("STEP-C", OnFailure.HALT),
    )
    registry = (
        ExecutionFilterRegistry()
        .register("STEP-A", _recording_filter(invoked, Outcome.PASSED))
        .register("STEP-B", _recording_filter(invoked, Outcome.PASSED))
        .register("STEP-C", _recording_filter(invoked, Outcome.PASSED))
    )

    PipelineBuilder(euc, registry).run(PipelineContext())

    assert invoked == ["STEP-A", "STEP-B", "STEP-C"]


def test_failed_requirement_with_halt_stops_the_run_before_the_next_one():
    invoked: list[str] = []
    euc = _euc_with(
        _requirement("STEP-A", OnFailure.HALT),
        _requirement("STEP-B", OnFailure.HALT),
        _requirement("STEP-C", OnFailure.HALT),
    )
    registry = (
        ExecutionFilterRegistry()
        .register("STEP-A", _recording_filter(invoked, Outcome.PASSED))
        .register("STEP-B", _recording_filter(invoked, Outcome.FAILED))
        .register("STEP-C", _recording_filter(invoked, Outcome.PASSED))
    )

    PipelineBuilder(euc, registry).run(PipelineContext())

    assert invoked == ["STEP-A", "STEP-B"], "STEP-C must not run once STEP-B halts the pipeline"


def test_failed_requirement_with_continue_does_not_stop_the_run():
    invoked: list[str] = []
    euc = _euc_with(
        _requirement("STEP-A", OnFailure.HALT),
        _requirement("STEP-B", OnFailure.CONTINUE),
        _requirement("STEP-C", OnFailure.HALT),
    )
    registry = (
        ExecutionFilterRegistry()
        .register("STEP-A", _recording_filter(invoked, Outcome.PASSED))
        .register("STEP-B", _recording_filter(invoked, Outcome.FAILED))
        .register("STEP-C", _recording_filter(invoked, Outcome.PASSED))
    )

    PipelineBuilder(euc, registry).run(PipelineContext())

    assert invoked == ["STEP-A", "STEP-B", "STEP-C"], "onFailure: continue must not halt the pipeline"


def test_filters_read_and_write_the_same_pipeline_context_passed_to_run():
    euc = _euc_with(_requirement("STEP-A", OnFailure.HALT))

    def filter_fn(context: PipelineContext, stage: ExecutionRequirement) -> Outcome:
        context.put("marker", "written-by-STEP-A")
        return Outcome.PASSED

    registry = ExecutionFilterRegistry().register("STEP-A", filter_fn)
    context = PipelineContext()

    PipelineBuilder(euc, registry).run(context)

    assert context.get("marker") == "written-by-STEP-A"
