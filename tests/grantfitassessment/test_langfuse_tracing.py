"""Offline tests for the Langfuse tracing wrappers — no credentials, no
network. A fake client double records what would have been sent, so these
assert the wrappers delegate correctly and emit the right shape of
observation/score calls without needing a real Langfuse connection.
"""

from __future__ import annotations

from euc.core.loader import load_grant_fit_assessment
from euc.grantfitassessment.app import GrantFitApplication
from euc.grantfitassessment.eval.dataset import TestCase
from euc.grantfitassessment.eval.evaluator import GrantFitEvaluator
from euc.grantfitassessment.langfuse_tracing import (
    TracedFitReasoner,
    TracedGrantFitEvaluator,
    traced_filter_wrapper,
)
from euc.grantfitassessment.models import AssessmentResult, GrantOpportunity, Organization
from tests.grantfitassessment.fakes import FakeFitReasoner

_euc = load_grant_fit_assessment()


class _FakeObservation:
    def __init__(self) -> None:
        self.output = None
        self.usage_details = None

    def update(self, output=None, usage_details=None, **kwargs):
        self.output = output
        self.usage_details = usage_details


class _FakeObservationContext:
    def __init__(self, calls: list, kwargs: dict) -> None:
        self._calls = calls
        self._kwargs = kwargs
        self.observation = _FakeObservation()

    def __enter__(self):
        self._calls.append(self._kwargs)
        return self.observation

    def __exit__(self, *exc_info):
        return False


class FakeLangfuseClient:
    def __init__(self) -> None:
        self.observations: list[dict] = []
        self.scores: list[dict] = []
        self._contexts: list[_FakeObservationContext] = []

    def start_as_current_observation(self, **kwargs):
        ctx = _FakeObservationContext(self.observations, kwargs)
        self._contexts.append(ctx)
        return ctx

    def score_current_trace(self, **kwargs):
        self.scores.append(kwargs)

    def flush(self):
        pass


def _org(**overrides) -> Organization:
    defaults = dict(
        name="Riverside Youth Coalition",
        mission_statement="Providing after-school STEM programs to underserved youth.",
        programs=["STEM tutoring"],
        operating_region="Pacific Northwest",
        is_registered_nonprofit=True,
    )
    defaults.update(overrides)
    return Organization(**defaults)


def _grant(**overrides) -> GrantOpportunity:
    defaults = dict(
        funder_name="Evergreen Community Foundation",
        grant_name="Youth Education Grant",
        funding_priorities=["STEM education"],
        eligibility_requirements=[],
        allowed_regions=["Pacific Northwest"],
        requires_registered_nonprofit=True,
    )
    defaults.update(overrides)
    return GrantOpportunity(**defaults)


def test_traced_fit_reasoner_delegates_and_returns_the_same_result():
    fake_client = FakeLangfuseClient()
    reasoner = FakeFitReasoner.returning("STRONG_FIT", "explanation", ["evidence"], [])
    traced = TracedFitReasoner(reasoner, client=fake_client)

    result = traced.assess_fit(_org(), _grant(), _euc)

    assert reasoner.was_invoked()
    assert result.fit_classification == "STRONG_FIT"
    assert result.supporting_evidence == ["evidence"]


def test_traced_fit_reasoner_opens_a_generation_with_a_stable_verb_first_name():
    fake_client = FakeLangfuseClient()
    reasoner = FakeFitReasoner.returning("STRONG_FIT", "explanation", ["evidence"], [])
    traced = TracedFitReasoner(reasoner, client=fake_client)

    traced.assess_fit(_org(), _grant(), _euc)

    assert len(fake_client.observations) == 1
    call = fake_client.observations[0]
    assert call["as_type"] == "generation"
    assert call["name"] == "assess-alignment"


def test_traced_fit_reasoner_forwards_token_usage_when_the_wrapped_reasoner_has_it():
    fake_client = FakeLangfuseClient()
    reasoner = FakeFitReasoner.returning("STRONG_FIT", "explanation", ["evidence"], [])
    reasoner.last_usage = {"input": 512, "output": 128}
    traced = TracedFitReasoner(reasoner, client=fake_client)

    traced.assess_fit(_org(), _grant(), _euc)

    assert fake_client.observations[0]  # observation opened
    ctx = fake_client._contexts[0]
    assert ctx.observation.usage_details == {"input": 512, "output": 128}


def test_traced_evaluator_delegates_and_returns_the_same_score():
    fake_client = FakeLangfuseClient()
    evaluator = TracedGrantFitEvaluator(GrantFitEvaluator(_euc), client=fake_client)
    tc = TestCase(
        case_id="case-1",
        organization=_org(),
        grant=_grant(),
        expected_eligible=True,
        expected_fit_classification="STRONG_FIT",
        ground_truth_rationale="rationale",
        expected_evidence_keywords=["STEM"],
    )
    actual = AssessmentResult(
        eligible=True,
        failed_eligibility_rules=[],
        fit_classification="STRONG_FIT",
        explanation="explanation",
        supporting_evidence=["Strong STEM alignment"],
        identified_uncertainty=[],
    )

    score = evaluator.evaluate(tc, actual)

    assert score.all_passed()


def test_traced_evaluator_emits_one_boolean_score_per_criterion():
    fake_client = FakeLangfuseClient()
    evaluator = TracedGrantFitEvaluator(GrantFitEvaluator(_euc), client=fake_client)
    tc = TestCase(
        case_id="case-1",
        organization=_org(),
        grant=_grant(),
        expected_eligible=True,
        expected_fit_classification="STRONG_FIT",
        ground_truth_rationale="rationale",
        expected_evidence_keywords=["STEM"],
    )
    actual = AssessmentResult(
        eligible=True,
        failed_eligibility_rules=[],
        fit_classification="STRONG_FIT",
        explanation="explanation",
        supporting_evidence=["Strong STEM alignment"],
        identified_uncertainty=[],
    )

    evaluator.evaluate(tc, actual)

    assert len(fake_client.scores) == 3
    names = {s["name"] for s in fake_client.scores}
    assert names == {"EVAL-ELIGIBILITY", "EVAL-ALIGNMENT", "EVAL-EVIDENCE"}
    for s in fake_client.scores:
        assert s["data_type"] == "BOOLEAN"
        assert s["value"] == 1.0


def test_traced_evaluator_reports_missing_keywords_in_the_score_comment():
    fake_client = FakeLangfuseClient()
    evaluator = TracedGrantFitEvaluator(GrantFitEvaluator(_euc), client=fake_client)
    tc = TestCase(
        case_id="case-1",
        organization=_org(),
        grant=_grant(),
        expected_eligible=True,
        expected_fit_classification="STRONG_FIT",
        ground_truth_rationale="rationale",
        expected_evidence_keywords=["STEM", "underserved"],
    )
    actual = AssessmentResult(
        eligible=True,
        failed_eligibility_rules=[],
        fit_classification="STRONG_FIT",
        explanation="explanation",
        supporting_evidence=["Some unrelated evidence"],
        identified_uncertainty=[],
    )

    evaluator.evaluate(tc, actual)

    evidence_score = next(s for s in fake_client.scores if s["name"] == "EVAL-EVIDENCE")
    assert evidence_score["value"] == 0.0
    assert "STEM" in evidence_score["comment"]


def test_traced_filter_wrapper_traces_each_deterministic_gate_as_a_verb_first_tool():
    fake_client = FakeLangfuseClient()
    reasoner = FakeFitReasoner.returning("STRONG_FIT", "explanation", ["evidence"], [])
    app = GrantFitApplication(_euc, reasoner, filter_wrapper=traced_filter_wrapper(fake_client))

    app.assess(_org(), _grant())

    names = [c["name"] for c in fake_client.observations]
    as_types = {c["name"]: c["as_type"] for c in fake_client.observations}
    assert names == ["verify-eligibility", "verify-geography", "verify-info"]
    assert all(t == "tool" for t in as_types.values())


def test_traced_filter_wrapper_puts_the_requirement_id_in_metadata():
    fake_client = FakeLangfuseClient()
    reasoner = FakeFitReasoner.returning("STRONG_FIT", "explanation", ["evidence"], [])
    app = GrantFitApplication(_euc, reasoner, filter_wrapper=traced_filter_wrapper(fake_client))

    app.assess(_org(), _grant())

    by_name = {c["name"]: c["metadata"] for c in fake_client.observations}
    assert by_name["verify-eligibility"] == {"requirementId": "ELIGIBILITY-001"}
    assert by_name["verify-geography"] == {"requirementId": "GEOGRAPHY-001"}
    assert by_name["verify-info"] == {"requirementId": "INFO-001"}


def test_traced_filter_wrapper_does_not_double_trace_alignment():
    # ALIGNMENT-001 is traced as a `generation` by TracedFitReasoner
    # already; traced_filter_wrapper must not also wrap it as a `tool`.
    fake_client = FakeLangfuseClient()
    reasoner = FakeFitReasoner.returning("STRONG_FIT", "explanation", ["evidence"], [])
    app = GrantFitApplication(_euc, reasoner, filter_wrapper=traced_filter_wrapper(fake_client))

    app.assess(_org(), _grant())

    names = [c["name"] for c in fake_client.observations]
    assert "ALIGNMENT-001" not in names
    assert not any("alignment" in n for n in names), "alignment is traced as a generation, not a tool"


def test_traced_filter_wrapper_stops_emitting_observations_after_a_halt():
    fake_client = FakeLangfuseClient()
    reasoner = FakeFitReasoner.that_must_not_be_invoked()
    app = GrantFitApplication(_euc, reasoner, filter_wrapper=traced_filter_wrapper(fake_client))

    # Not a registered nonprofit -> halts at ELIGIBILITY-001.
    ineligible_org = _org(is_registered_nonprofit=False)
    app.assess(ineligible_org, _grant())

    names = [c["name"] for c in fake_client.observations]
    assert names == ["verify-eligibility"], "verify-geography/verify-info must not run once eligibility halts"
