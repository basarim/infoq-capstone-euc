"""EVAL-ELIGIBILITY as a DeepEval custom deterministic metric — pure equality
against ground truth, same as eligibility_correctness_filter. An LLM judge
has no role here: there is nothing to interpret."""

from __future__ import annotations

from deepeval.metrics import BaseMetric
from deepeval.test_case import LLMTestCase


class EligibilityCorrectnessMetric(BaseMetric):
    def __init__(self, threshold: float = 1.0) -> None:
        self.threshold = threshold

    def measure(self, test_case: LLMTestCase) -> float:
        actual = test_case.metadata["eligible"]
        expected = test_case.metadata["expected_eligible"]

        self.score = 1.0 if actual == expected else 0.0
        self.reason = f"actual eligible={actual}, expected eligible={expected}"
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
        return "Eligibility Correctness"
