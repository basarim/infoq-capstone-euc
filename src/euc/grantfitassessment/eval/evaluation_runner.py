"""A simple, single-model batch scorer for the golden set: one baseline pass,
printed to stdout, no file output. For the multi-variant controlled-change
experiment, see drift_experiment_main.py instead."""

from __future__ import annotations

import os

from euc.core.loader import load_grant_fit_assessment
from euc.grantfitassessment.app import GrantFitApplication
from euc.grantfitassessment.eval import dataset as dataset_module
from euc.grantfitassessment.eval.evaluator import GrantFitEvaluator
from euc.grantfitassessment.reasoner import LlmFitReasoner


def main() -> None:
    euc = load_grant_fit_assessment()

    model_name = os.environ.get("LLM_MODEL", "claude-sonnet-4-6")
    app = GrantFitApplication(euc, LlmFitReasoner(model_name))
    evaluator = GrantFitEvaluator(euc)

    test_cases = dataset_module.load_from_file(dataset_module.DATASET_PATH)

    passed = 0
    for test_case in test_cases:
        try:
            actual = app.assess(test_case.organization, test_case.grant)
            score = evaluator.evaluate(test_case, actual)
            print(f"{test_case.case_id} -> {score}")
            if score.all_passed():
                passed += 1
        except NotImplementedError as e:
            print(f"{test_case.case_id} -> reasoning not yet available: {e}")

    print(f"\n{passed} / {len(test_cases)} test cases passed all criteria")


if __name__ == "__main__":
    main()
