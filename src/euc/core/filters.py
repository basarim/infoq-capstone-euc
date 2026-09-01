"""The seam between an EUC's declared requirements/criteria and whatever code
carries them out. A filter is registered against the id of the requirement or
criterion it implements, so the EUC never has to name a class."""

from __future__ import annotations

from enum import Enum
from typing import Callable, Protocol

from euc.core.context import PipelineContext
from euc.core.models import EvaluationCriterion, ExecutionRequirement


class Outcome(str, Enum):
    PASSED = "PASSED"
    FAILED = "FAILED"


class Verdict(str, Enum):
    PASSED = "PASSED"
    FAILED = "FAILED"


class ExecutionFilter(Protocol):
    """Returning FAILED only halts the run if the requirement's onFailure is
    HALT — the filter doesn't decide that, the pipeline does."""

    def execute(self, context: PipelineContext, requirement: ExecutionRequirement) -> Outcome: ...


class EvaluationFilter(Protocol):
    def evaluate(self, context: PipelineContext, criterion: EvaluationCriterion) -> Verdict: ...


ExecutionFilterFn = Callable[[PipelineContext, ExecutionRequirement], Outcome]
EvaluationFilterFn = Callable[[PipelineContext, EvaluationCriterion], Verdict]


class ExecutionFilterRegistry:
    def __init__(self) -> None:
        self._by_requirement_id: dict[str, ExecutionFilterFn] = {}

    def register(self, requirement_id: str, filter_fn: ExecutionFilterFn) -> "ExecutionFilterRegistry":
        self._by_requirement_id[requirement_id] = filter_fn
        return self

    def get(self, requirement_id: str) -> ExecutionFilterFn:
        filter_fn = self._by_requirement_id.get(requirement_id)
        if filter_fn is None:
            raise RuntimeError(f"No implementation registered for execution requirement '{requirement_id}'")
        return filter_fn


class EvaluationFilterRegistry:
    def __init__(self) -> None:
        self._by_criterion_id: dict[str, EvaluationFilterFn] = {}

    def register(self, criterion_id: str, filter_fn: EvaluationFilterFn) -> "EvaluationFilterRegistry":
        self._by_criterion_id[criterion_id] = filter_fn
        return self

    def get(self, criterion_id: str) -> EvaluationFilterFn:
        filter_fn = self._by_criterion_id.get(criterion_id)
        if filter_fn is None:
            raise RuntimeError(f"No evaluator registered for criterion '{criterion_id}'")
        return filter_fn
