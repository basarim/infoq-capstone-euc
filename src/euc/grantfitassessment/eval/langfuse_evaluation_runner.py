"""The Langfuse counterpart to evaluation_runner.py — same golden set, same
GrantFitApplication and bespoke GrantFitEvaluator, but wrapped so every case
becomes one Langfuse trace: a stable-named span per case (case id in
metadata, not the name — see langfuse_tracing.py for why), a `tool`
observation per deterministic gate that actually runs, a nested `generation`
for the Claude call, and the three evaluation criteria attached as scores.

Each request (one test case run through the application) gets a fresh
correlation id, and every trace carries both that correlation id and the
EUC's own id — the correlation id as metadata (per-request, dynamic), the
EUC id as a tag (stable across every run of this EUC, known upfront), set
together via `propagate_attributes()` so they land on the root span and
every observation nested under it.

Requires ANTHROPIC_API_KEY (for the reasoning step) and, to actually see
anything in a Langfuse project, LANGFUSE_PUBLIC_KEY and LANGFUSE_SECRET_KEY.
Without those two, the Langfuse client runs disabled — every call in this
file still completes without raising, but nothing is sent anywhere.
Optionally set LANGFUSE_TRACING_ENVIRONMENT (e.g. "development") so these
evaluation-run traces are distinguishable from any other environment
sending traces to the same project.
"""

from __future__ import annotations

import os
import uuid

from langfuse import get_client, propagate_attributes

from euc.core.loader import load_grant_fit_assessment
from euc.grantfitassessment.app import GrantFitApplication
from euc.grantfitassessment.eval import dataset as dataset_module
from euc.grantfitassessment.eval.evaluator import GrantFitEvaluator
from euc.grantfitassessment.langfuse_tracing import (
    TracedFitReasoner,
    TracedGrantFitEvaluator,
    traced_filter_wrapper,
)
from euc.grantfitassessment.reasoner import LlmFitReasoner


def main() -> None:
    euc = load_grant_fit_assessment()
    client = get_client()

    model_name = os.environ.get("LLM_MODEL", "claude-sonnet-4-6")
    reasoner = TracedFitReasoner(LlmFitReasoner(model_name), client=client)
    app = GrantFitApplication(euc, reasoner, filter_wrapper=traced_filter_wrapper(client))
    evaluator = TracedGrantFitEvaluator(GrantFitEvaluator(euc), client=client)

    test_cases = dataset_module.load_from_file(dataset_module.DATASET_PATH)

    passed = 0
    for test_case in test_cases:
        correlation_id = str(uuid.uuid4())
        with propagate_attributes(tags=[euc.id], metadata={"correlationId": correlation_id}):
            with client.start_as_current_observation(
                as_type="span",
                name="assess-grant-fit",
                input={"organization": test_case.organization.name, "grant": test_case.grant.grant_name},
                metadata={
                    "caseId": test_case.case_id,
                    "expectedEligible": test_case.expected_eligible,
                    "expectedFitClassification": test_case.expected_fit_classification,
                },
            ) as span:
                actual = app.assess(test_case.organization, test_case.grant)
                score = evaluator.evaluate(test_case, actual)
                span.update(output={"eligible": actual.eligible, "fitClassification": actual.fit_classification})

        print(f"{test_case.case_id} [correlation_id={correlation_id}] -> {score}")
        if score.all_passed():
            passed += 1

    client.flush()
    print(f"\n{passed} / {len(test_cases)} test cases passed all criteria")


if __name__ == "__main__":
    main()
