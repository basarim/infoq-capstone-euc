"""Runs a controlled-change experiment: a baseline reasoner plus one or more
candidate variants, scored against the same dataset, to see whether the
mapped evaluators can tell a safe implementation change from a business
regression. Requires ANTHROPIC_API_KEY (used transitively by LlmFitReasoner)."""

from __future__ import annotations

import os
from datetime import datetime, timezone

from euc.core.loader import load_grant_fit_assessment
from euc.grantfitassessment.eval import dataset as dataset_module
from euc.grantfitassessment.eval.drift_experiment_report_writer import write_json
from euc.grantfitassessment.eval.drift_experiment_runner import DriftExperimentRunner
from euc.grantfitassessment.eval.fit_reasoner_variant import FitReasonerVariant
from euc.grantfitassessment.reasoner import AlternateAlignmentPromptReasoner, LlmFitReasoner

RESULTS_DIR = "eval/grant-fit-assessment/results"


def main() -> None:
    euc = load_grant_fit_assessment()
    dataset = dataset_module.load_from_file(dataset_module.DATASET_PATH)

    baseline_model = os.environ.get("LLM_MODEL", "claude-sonnet-4-6")
    baseline = FitReasonerVariant(
        label=f"baseline:{baseline_model}",
        reasoner=LlmFitReasoner(baseline_model),
        expected_to_alter_behavior=False,
    )

    candidates = [
        FitReasonerVariant(
            label="prompt-variant:loosened-alignment-instructions",
            reasoner=AlternateAlignmentPromptReasoner(baseline_model),
            expected_to_alter_behavior=True,
        )
    ]

    alternate_model = os.environ.get("LLM_MODEL_VARIANT")
    if alternate_model and alternate_model.strip():
        candidates.append(
            FitReasonerVariant(
                label=f"model-variant:{alternate_model}",
                reasoner=LlmFitReasoner(alternate_model),
                expected_to_alter_behavior=True,
            )
        )

    runner = DriftExperimentRunner(euc, dataset)
    report = runner.run(baseline, candidates)
    print(report.summary())

    timestamp = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z").replace(":", "-")
    output_file = f"{RESULTS_DIR}/drift-experiment-{timestamp}.json"
    write_json(report, output_file)
    print(f"Wrote {output_file}")


if __name__ == "__main__":
    main()
