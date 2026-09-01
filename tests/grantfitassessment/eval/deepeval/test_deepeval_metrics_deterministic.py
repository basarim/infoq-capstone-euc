"""Offline tests for the two deterministic DeepEval metrics — no API key, no
network call. Mirrors the equivalent cases in test_grant_fit_evaluator.py to
confirm the DeepEval port agrees with the bespoke evaluator's logic.

The evidence-grounding GEval metric needs a live Claude call and is
deliberately not covered here — see deepeval_evaluation_runner.py for how to
exercise it live, and run `deepeval test run` against this file to also
confirm it collects correctly under DeepEval's own test runner.
"""

from __future__ import annotations

from deepeval.test_case import LLMTestCase

from euc.grantfitassessment.eval.deepeval.eligibility_correctness_metric import (
    EligibilityCorrectnessMetric,
)
from euc.grantfitassessment.eval.deepeval.program_alignment_metric import ProgramAlignmentMetric


def _deterministic_case(*, eligible: bool, expected_eligible: bool, fit: str, expected_fit: str) -> LLMTestCase:
    return LLMTestCase(
        input="Assess whether Org is eligible for and a good fit for Grant.",
        actual_output=fit,
        expected_output=expected_fit,
        metadata={
            "eligible": eligible,
            "expected_eligible": expected_eligible,
            "fit_classification": fit,
        },
    )


def test_eligibility_correctness_passes_when_actual_matches_expected():
    metric = EligibilityCorrectnessMetric()
    test_case = _deterministic_case(eligible=True, expected_eligible=True, fit="STRONG_FIT", expected_fit="STRONG_FIT")

    metric.measure(test_case)

    assert metric.is_successful()


def test_eligibility_correctness_fails_on_mismatch():
    metric = EligibilityCorrectnessMetric()
    test_case = _deterministic_case(eligible=False, expected_eligible=True, fit="POOR_FIT", expected_fit="STRONG_FIT")

    metric.measure(test_case)

    assert not metric.is_successful()


def test_program_alignment_passes_when_classification_matches():
    metric = ProgramAlignmentMetric()
    test_case = _deterministic_case(eligible=True, expected_eligible=True, fit="STRONG_FIT", expected_fit="STRONG_FIT")

    metric.measure(test_case)

    assert metric.is_successful()


def test_program_alignment_fails_on_mismatched_classification():
    metric = ProgramAlignmentMetric()
    test_case = _deterministic_case(eligible=True, expected_eligible=True, fit="POOR_FIT", expected_fit="STRONG_FIT")

    metric.measure(test_case)

    assert not metric.is_successful()


def test_program_alignment_vacuously_passes_when_ineligible_even_if_classification_differs():
    metric = ProgramAlignmentMetric()
    test_case = _deterministic_case(eligible=False, expected_eligible=False, fit="POOR_FIT", expected_fit="STRONG_FIT")

    metric.measure(test_case)

    assert metric.is_successful(), "an ineligible verdict vacuously passes — fit reasoning never ran"
