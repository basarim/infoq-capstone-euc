"""The DeepEval counterpart to evaluation_runner.py — same golden set, same
GrantFitApplication, but scored by DeepEvalGrantFitEvaluator instead of the
bespoke GrantFitEvaluator. The concrete demonstration that the evaluation
framework can change under an unchanged EUC: only this file's evaluator
import differs from evaluation_runner.py.

Requires ANTHROPIC_API_KEY (used by both the application's LlmFitReasoner and
the DeepEval evidence-grounding metric's Claude judge model).
"""

from __future__ import annotations

import os

from euc.core.loader import load_grant_fit_assessment
from euc.grantfitassessment.app import GrantFitApplication
from euc.grantfitassessment.eval import dataset as dataset_module
from euc.grantfitassessment.eval.deepeval_evaluator import DeepEvalGrantFitEvaluator
from euc.grantfitassessment.reasoner import LlmFitReasoner


def main() -> None:
    euc = load_grant_fit_assessment()

    model_name = os.environ.get("LLM_MODEL", "claude-sonnet-4-6")
    app = GrantFitApplication(euc, LlmFitReasoner(model_name))
    evaluator = DeepEvalGrantFitEvaluator(euc, model_name)

    test_cases = dataset_module.load_from_file(dataset_module.DATASET_PATH)

    passed = 0
    for test_case in test_cases:
        actual = app.assess(test_case.organization, test_case.grant)
        score = evaluator.evaluate(test_case, actual)
        print(f"{test_case.case_id} -> {score}")
        if score.all_passed():
            passed += 1

    print(f"\n{passed} / {len(test_cases)} test cases passed all criteria")


if __name__ == "__main__":
    main()
