"""Proves the drift-experiment mechanics (docs/proposal.md Section 7) work
offline, with fake FitReasoners standing in for real model/prompt variants —
no ANTHROPIC_API_KEY needed. A live run (drift_experiment_main.py) still
needs a real key; this test only verifies the metric computation itself is
correct once outcomes come back.
"""

from __future__ import annotations

import math

from euc.core.loader import load_grant_fit_assessment
from euc.grantfitassessment.eval.dataset import TestCase
from euc.grantfitassessment.eval.drift_experiment_runner import DriftExperimentRunner
from euc.grantfitassessment.eval.fit_reasoner_variant import FitReasonerVariant
from euc.grantfitassessment.models import GrantOpportunity, Organization
from tests.grantfitassessment.fakes import FakeFitReasoner

_euc = load_grant_fit_assessment()


def _eligible_ground_truth(case_id: str, expected_fit: str, expected_keywords: list[str]) -> TestCase:
    return TestCase(
        case_id=case_id,
        organization=Organization(name="Org", mission_statement="Mission", programs=["Program"], operating_region="PNW", is_registered_nonprofit=True),
        grant=GrantOpportunity(
            funder_name="Funder", grant_name="Grant", funding_priorities=["Priority"],
            eligibility_requirements=[], allowed_regions=["PNW"], requires_registered_nonprofit=True,
        ),
        expected_eligible=True,
        expected_fit_classification=expected_fit,
        ground_truth_rationale="rationale",
        expected_evidence_keywords=expected_keywords,
    )


def _reasoner_always_returning(fit_classification: str, evidence: list[str]) -> FakeFitReasoner:
    return FakeFitReasoner.returning(fit_classification, "explanation", evidence, [])


def test_behavior_altering_variant_that_disagrees_with_ground_truth_is_flagged_as_drift():
    dataset = [_eligible_ground_truth("case-1", "STRONG_FIT", ["STEM"])]

    baseline = FitReasonerVariant("baseline", _reasoner_always_returning("STRONG_FIT", ["STEM alignment"]), False)
    drifted_variant = FitReasonerVariant("drifted-model", _reasoner_always_returning("POOR_FIT", ["STEM alignment"]), True)

    report = DriftExperimentRunner(_euc, dataset).run(baseline, [drifted_variant])

    assert report.drift_detection_rate == 1.0, "the one altering variant should have been flagged"
    assert math.isnan(report.false_flag_rate), "no neutral variants were run"
    assert len(report.variant_results) == 1
    assert report.variant_results[0].any_drift_flagged()


def test_behavior_neutral_variant_that_agrees_with_ground_truth_is_not_flagged():
    dataset = [_eligible_ground_truth("case-1", "STRONG_FIT", ["STEM"])]

    baseline = FitReasonerVariant("baseline", _reasoner_always_returning("STRONG_FIT", ["STEM alignment"]), False)
    neutral_variant = FitReasonerVariant("harmless-prompt-tweak", _reasoner_always_returning("STRONG_FIT", ["STEM alignment"]), False)

    report = DriftExperimentRunner(_euc, dataset).run(baseline, [neutral_variant])

    assert report.false_flag_rate == 0.0, "the neutral variant should not have been flagged"
    assert math.isnan(report.drift_detection_rate), "no altering variants were run"
    assert not report.variant_results[0].any_drift_flagged()


def test_deterministic_rule_stability_holds_when_only_reasoning_layer_changes():
    # eligibility/geography/requiredInfo never touch the FitReasoner, so
    # eligibilityCorrectness must stay identical across every variant
    # regardless of what the reasoner returns.
    dataset = [_eligible_ground_truth("case-1", "STRONG_FIT", [])]

    baseline = FitReasonerVariant("baseline", _reasoner_always_returning("STRONG_FIT", []), False)
    variant = FitReasonerVariant("any-reasoning-variant", _reasoner_always_returning("POOR_FIT", []), True)

    report = DriftExperimentRunner(_euc, dataset).run(baseline, [variant])

    assert report.deterministic_rule_stability_rate == 1.0, (
        "eligibilityCorrectness must not move when only alignmentReasoning changes"
    )


def test_evidence_grounding_consistency_reflects_fraction_of_grounded_eligible_cases():
    dataset = [
        _eligible_ground_truth("grounded", "STRONG_FIT", ["STEM"]),
        _eligible_ground_truth("ungrounded", "STRONG_FIT", ["STEM"]),
    ]

    baseline = FitReasonerVariant("baseline", _reasoner_always_returning("STRONG_FIT", ["STEM alignment"]), False)

    # A variant that only ever cites unrelated evidence -> fails
    # evidenceGrounding on both cases -> consistency rate should be 0.0.
    ungrounded_variant = FitReasonerVariant("ungrounded-variant", _reasoner_always_returning("STRONG_FIT", ["unrelated note"]), True)

    report = DriftExperimentRunner(_euc, dataset).run(baseline, [ungrounded_variant])

    assert report.evidence_grounding_consistency_rate == 0.0
