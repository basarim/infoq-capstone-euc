"""EVAL-ALIGNMENT as a DeepEval custom deterministic metric — checked against
the assessment's own (actual) eligibility, not ground-truth eligibility: an
ineligible verdict vacuously passes, since fit reasoning never ran to
produce one worth checking. Same rule as program_alignment_filter."""

from __future__ import annotations

from deepeval.metrics import BaseMetric
from deepeval.test_case import LLMTestCase


class ProgramAlignmentMetric(BaseMetric):
    def __init__(self, threshold: float = 1.0) -> None:
        self.threshold = threshold

    def measure(self, test_case: LLMTestCase) -> float:
        if not test_case.metadata["eligible"]:
            self.score = 1.0
            self.reason = "Ineligible — fit reasoning never ran, so alignment vacuously passes."
            self.success = True
            return self.score

        actual_fit = test_case.metadata["fit_classification"]
        expected_fit = test_case.expected_output

        self.score = 1.0 if actual_fit == expected_fit else 0.0
        self.reason = f"actual fit={actual_fit!r}, expected fit={expected_fit!r}"
        self.success = self.score >= self.threshold
        return self.score

    async def a_measure(self, test_case: LLMTestCase) -> float:
        return self.measure(test_case)

    def is_successful(self) -> bool:
        if self.error is not None:
            self.success = False
        else:
            self.success = self.score is not None and self.score >= self.threshold
        return self.success

    @property
    def __name__(self) -> str:
        return "Program Alignment"
