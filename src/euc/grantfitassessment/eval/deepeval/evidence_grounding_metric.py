"""EVAL-EVIDENCE as a genuine GEval metric, judged by Claude.

The bespoke evaluator's evidence_grounding_filter checks this with a
case-insensitive keyword substring match — a crude proxy for whether the
explanation actually cites real evidence. What POLICY-EVIDENCE,
POLICY-MISSING-DATA, and POLICY-UNCERTAINTY actually ask for is a judgment
call: does the explanation stay grounded in the organization's real profile,
avoid inventing anything not present, and flag uncertainty rather than
guessing confidently? That's a task for an LLM judge, not a substring check.
"""

from __future__ import annotations

from deepeval.metrics import GEval
from deepeval.test_case import LLMTestCase, SingleTurnParams

from euc.grantfitassessment.eval.deepeval.claude_judge_model import ClaudeJudgeModel

_CRITERIA = (
    "Determine whether the explanation in 'actual output' is genuinely grounded in "
    "the organization's profile and the supporting evidence listed in 'context', "
    "without inventing facts that are not present there. The explanation should cite "
    "specific evidence rather than making vague or unsupported claims, and should "
    "flag uncertainty explicitly rather than guessing with false confidence."
)


def build_evidence_grounding_metric(model_name: str, threshold: float = 0.5) -> GEval:
    return GEval(
        name="Evidence Grounding",
        criteria=_CRITERIA,
        evaluation_params=[
            SingleTurnParams.INPUT,
            SingleTurnParams.ACTUAL_OUTPUT,
            SingleTurnParams.CONTEXT,
        ],
        model=ClaudeJudgeModel(model_name),
        threshold=threshold,
    )


def evidence_grounding_test_case(
    input_description: str, explanation: str, supporting_evidence: list[str]
) -> LLMTestCase:
    return LLMTestCase(
        input=input_description,
        actual_output=explanation,
        context=supporting_evidence or None,
    )
