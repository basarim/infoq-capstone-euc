"""Runs an EUC's execution requirements in declared order, resolving each one
to its implementation through an ExecutionFilterRegistry.

There is no orchestration logic specific to any use case here: the EUC says
what must happen and in what order, the registry says what code carries each
step out, and this module does nothing but compile that list into a graph and
walk it. The engine is a LangGraph StateGraph — one node per execution
requirement, connected by conditional edges — built generically from
euc.execution_requirements, so no requirement id or business term appears here.

A requirement whose outcome is FAILED and whose onFailure policy is HALT
routes straight to the graph's END instead of the next requirement. That is
the business contract being honoured — "strong alignment cannot overcome a
failed mandatory requirement" is enforced here because the EUC says so, not
because application code happens to be written that way.

The graph's own state carries no business data: each filter still reads and
writes the PipelineContext instance passed into run(), exactly as before —
LangGraph passes state through by reference between nodes in the same
process, so no cloning/serialization workaround is needed here the way one
was needed on the JVM.
"""

from __future__ import annotations

from typing import Any, TypedDict

from langgraph.graph import END, START, StateGraph

from euc.core.context import PipelineContext
from euc.core.filters import ExecutionFilterRegistry, Outcome
from euc.core.models import EucDefinition, OnFailure

_PIPELINE_CONTEXT = "pipeline_context"
_ROUTE = "route"
_CONTINUE = "continue"
_HALT = "halt"


class _RouteState(TypedDict):
    pipeline_context: Any
    route: str


class PipelineBuilder:
    def __init__(self, euc: EucDefinition, registry: ExecutionFilterRegistry) -> None:
        self._graph = _build_graph(euc, registry)

    def run(self, context: PipelineContext) -> None:
        result = self._graph.invoke({_PIPELINE_CONTEXT: context, _ROUTE: ""})
        if result is None:
            raise RuntimeError("EUC pipeline graph produced no result")


def _build_graph(euc: EucDefinition, registry: ExecutionFilterRegistry):
    requirements = euc.execution_requirements
    graph: StateGraph = StateGraph(_RouteState)
    graph.add_edge(START, requirements[0].id)

    for i, requirement in enumerate(requirements):
        node_id = requirement.id
        next_id = requirements[i + 1].id if i + 1 < len(requirements) else END

        def node(state: _RouteState, requirement=requirement) -> dict:
            context: PipelineContext = state[_PIPELINE_CONTEXT]
            filter_fn = registry.get(requirement.id)
            outcome = filter_fn(context, requirement)
            return {_ROUTE: outcome.value}

        graph.add_node(node_id, node)

        def route(state: _RouteState, requirement=requirement) -> str:
            should_halt = state.get(_ROUTE, Outcome.PASSED.value) == Outcome.FAILED.value and (
                requirement.on_failure == OnFailure.HALT
            )
            return _HALT if should_halt else _CONTINUE

        graph.add_conditional_edges(node_id, route, {_CONTINUE: next_id, _HALT: END})

    return graph.compile()


class EvaluationPipelineBuilder:
    """Criteria carry no onFailure policy — every criterion is scored
    regardless of another's verdict, since each measures something
    independent rather than gating a shared outcome. A plain ordered loop,
    not a graph: there is nothing to route."""

    def __init__(self, euc: EucDefinition, registry) -> None:
        self._euc = euc
        self._registry = registry

    def run(self, context: PipelineContext) -> dict:
        verdicts: dict = {}
        for criterion in self._euc.evaluation_criteria:
            filter_fn = self._registry.get(criterion.id)
            verdicts[criterion.id] = filter_fn(context, criterion)
        return verdicts
