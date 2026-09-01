"""Optional Langfuse observability layer, wrapping a FitReasoner, an
evaluator, and (via GrantFitApplication's filter_wrapper hook) each
deterministic gate — without changing any of their behavior. Langfuse is not
an evaluation framework (that's DeepEval's job) and not an orchestration
engine (that's LangGraph's job) — it's the layer that lets a team see what a
run actually did: the prompt sent to Claude, the raw response and token
usage, which deterministic gates ran (and which didn't, because an earlier
one halted the run), and the same three evaluation criteria the EUC
declares, now visible as scores attached to that run's trace.

Naming and structure follow Langfuse's own instrumentation guidance
(fetched fresh from https://langfuse.com/docs/observability/best-practices,
per the langfuse skill's documentation-first rule, not implemented from
memory): observation names are stable, low-cardinality, and verb-first
(`verify-eligibility`, not `ELIGIBILITY-001` or a case id — dynamic and
id-shaped values go in metadata instead), each observation uses the most
specific as_type available (`generation` for the LLM call, `tool` for each
deterministic check — deliberately not `guardrail`, which Langfuse reserves
for content-safety/jailbreak protection, not business-rule validation), and
the root span's input/output are curated for a reviewer rather than raw
function args. The EUC id is a stable, known-upfront dimension, so it's a
trace tag; the correlation id is per-request, so it's trace metadata — both
set once via `propagate_attributes()` in langfuse_evaluation_runner.py so
every observation in the trace inherits them.

All wrappers accept an optional client so tests can inject a stub and stay
offline, matching this project's rule that the default pytest run never
needs live credentials.
"""

from __future__ import annotations

from typing import Any, Callable, Protocol

from euc.core.context import PipelineContext
from euc.core.filters import ExecutionFilterFn, Outcome
from euc.core.models import EucDefinition, ExecutionRequirement
from euc.grantfitassessment.eval.dataset import TestCase
from euc.grantfitassessment.eval.evaluator import EvaluationScore
from euc.grantfitassessment.models import AssessmentResult, GrantOpportunity, Organization
from euc.grantfitassessment.reasoner import FitReasoner, FitReasoning

# ALIGNMENT-001 is deliberately excluded from tool-wrapping in
# traced_filter_wrapper: it's already traced as a `generation` by
# TracedFitReasoner, nested one level deeper (inside the filter). Wrapping it
# again here would double-instrument the same step.
_ALREADY_TRACED_AS_GENERATION = "ALIGNMENT-001"


class _Evaluator(Protocol):
    def evaluate(self, test_case: TestCase, actual: AssessmentResult) -> EvaluationScore: ...


def _default_client() -> Any:
    from langfuse import get_client

    return get_client()


class TracedFitReasoner:
    """Wraps any FitReasoner, tracing its call as a Langfuse `generation` —
    including token usage when the wrapped reasoner exposes `last_usage`
    (LlmFitReasoner and its subclasses do)."""

    def __init__(self, reasoner: FitReasoner, client: Any = None) -> None:
        self._reasoner = reasoner
        self._client = client if client is not None else _default_client()

    def assess_fit(self, org: Organization, grant: GrantOpportunity, euc: EucDefinition) -> FitReasoning:
        model_name = getattr(self._reasoner, "model_name", None)
        with self._client.start_as_current_observation(
            as_type="generation",
            name="assess-alignment",
            model=model_name,
            input={
                "organization": org.name,
                "missionStatement": org.mission_statement,
                "programs": org.programs,
                "grant": grant.grant_name,
                "funder": grant.funder_name,
                "fundingPriorities": grant.funding_priorities,
            },
        ) as generation:
            reasoning = self._reasoner.assess_fit(org, grant, euc)
            usage = getattr(self._reasoner, "last_usage", None)
            generation.update(
                output={
                    "fitClassification": reasoning.fit_classification,
                    "explanation": reasoning.explanation,
                    "supportingEvidence": reasoning.supporting_evidence,
                    "identifiedUncertainty": reasoning.identified_uncertainty,
                },
                usage_details=usage,
            )
        return reasoning


class TracedGrantFitEvaluator:
    """Wraps any evaluator sharing GrantFitEvaluator's shape — the bespoke
    evaluator or DeepEvalGrantFitEvaluator — and scores its verdicts onto
    the current Langfuse trace, one score per criterion id."""

    def __init__(self, evaluator: _Evaluator, client: Any = None) -> None:
        self._evaluator = evaluator
        self._client = client if client is not None else _default_client()

    def evaluate(self, test_case: TestCase, actual: AssessmentResult) -> EvaluationScore:
        score = self._evaluator.evaluate(test_case, actual)

        self._client.score_current_trace(
            name="EVAL-ELIGIBILITY",
            value=1.0 if score.eligibility_correctness else 0.0,
            data_type="BOOLEAN",
        )
        self._client.score_current_trace(
            name="EVAL-ALIGNMENT",
            value=1.0 if score.program_alignment else 0.0,
            data_type="BOOLEAN",
        )
        self._client.score_current_trace(
            name="EVAL-EVIDENCE",
            value=1.0 if score.evidence_grounding else 0.0,
            data_type="BOOLEAN",
            comment=(
                "missing: " + ", ".join(score.missing_evidence_keywords)
                if score.missing_evidence_keywords
                else None
            ),
        )

        return score


def traced_filter_wrapper(client: Any = None) -> Callable[[str, ExecutionFilterFn], ExecutionFilterFn]:
    """Returns a filter_wrapper for GrantFitApplication's constructor hook:
    traces each deterministic gate as a `tool` observation named after its
    real EUC requirement id (ELIGIBILITY-001, GEOGRAPHY-001, INFO-001) —
    stable, low-cardinality names that also tie the trace directly back to
    the EUC's own vocabulary. A requirement that never runs because an
    earlier one halted the pipeline correctly produces no observation at
    all, making the halt visible in the trace tree rather than hidden."""
    client = client if client is not None else _default_client()

    def wrap(requirement_id: str, filter_fn: ExecutionFilterFn) -> ExecutionFilterFn:
        if requirement_id == _ALREADY_TRACED_AS_GENERATION:
            return filter_fn

        def wrapped(context: PipelineContext, requirement: ExecutionRequirement) -> Outcome:
            with client.start_as_current_observation(
                as_type="tool",
                name=_verb_first_tool_name(requirement_id),
                metadata={"requirementId": requirement_id},
            ) as obs:
                outcome = filter_fn(context, requirement)
                obs.update(output={"outcome": outcome.value})
            return outcome

        return wrapped

    return wrap


def _verb_first_tool_name(requirement_id: str) -> str:
    """Langfuse's naming guidance wants observation names verb-first
    (`classify-intent`, not a noun/id) — but the EUC requirement id
    (e.g. "ELIGIBILITY-001") is still worth keeping on the trace, so it
    moves to metadata instead of the name, same treatment as case_id on
    the root span."""
    subject = requirement_id.rsplit("-", 1)[0].lower()
    return f"verify-{subject}"
