# Executable Use Cases: Grant Fit Assessment

**InfoQ AI Engineering Certification — System-Building Project**
**Cohort:** [cohort-name] &nbsp;|&nbsp; **Participant:** Basari (solo)

> Repo naming convention per the Project Guide: rename this repo to
> `[cohort-name]-[team-name]` before final submission (e.g. `july-2026-ai-cohort-basari`).

---

## Theme

This project addresses the capstone theme **"Engineering on Shifting Ground: Building
Dependable Systems on Undependable Components."** The core engineering problem: an
AI model is non-deterministic per call and unstable over time (prompts, models, and
context all drift), yet applications built on it still need a dependable notion of
"correct behavior" that survives that instability.

## System Description

This system implements **Executable Use Cases (EUCs)** — machine-readable artifacts
that capture business intent (goal, rules, policies, execution requirements, and
evaluation criteria) as a single definition consumed by *both* the running application
and its evaluation suite, rather than two definitions maintained independently.

The system is validated through a **Grant Fit Assessment** application: given a
nonprofit's profile and a grant opportunity, it determines eligibility (deterministic)
and fit — `STRONG_FIT` / `POSSIBLE_FIT` / `POOR_FIT` (LLM-reasoned) — grounded entirely
in a single EUC definition.

The system is designed so that when the underlying prompt or model changes, the EUC
itself does not — and evaluation against that fixed EUC should still correctly detect
whether the application's behavior has drifted from business intent. That claim is the
thing this project tests. See [`docs/proposal.md`](docs/proposal.md) for the full design
rationale, falsifiable claims, and evaluation methodology.

## Architecture

```
                 Business Intent
                       |
                       v
              Executable Use Case (EUC)
                 /           \
                v             v
           Execution      Evaluation

  grant-fit-assessment.json   <- single source of truth
       |                                |
       v                                v
  GrantFitApplication              GrantFitEvaluator
  (deterministic rules             (scores reasoned result
   + LLM reasoning)                 against EUC criteria)
```

`euc.core` is a domain-agnostic engine (the EUC model, its loader, and both pipeline
builders); everything specific to the Grant Fit use case — code, resource, dataset,
and results — lives under one folder per EUC, so a second EUC can be added later as a
sibling package/folder without touching the engine.

Requirement sequencing on the execution side runs on a compiled
[LangGraph](https://github.com/langchain-ai/langgraph) `StateGraph` — one node per
execution requirement, wired by conditional edges so a failed mandatory requirement
routes straight to the graph's end instead of the next node. See
[`docs/proposal.md`](docs/proposal.md) Appendix A for why.

| Component | Responsibility | Location |
|---|---|---|
| `euc.core` | EUC model + loader, plus both pipeline builders — `PipelineBuilder` (execution, LangGraph-based) and `EvaluationPipelineBuilder` (evaluation, a plain ordered loop) — that assemble a filter chain mechanically from the EUC JSON (see [`docs/proposal.md`](docs/proposal.md) Section 4) | `src/euc/core` |
| `euc.grantfitassessment` | Grant Fit domain logic — deterministic eligibility checks + LLM-reasoned fit assessment, run by `GrantFitApplication` via `PipelineBuilder` against filters in `.pipeline`; evaluation scored by `GrantFitEvaluator` via `EvaluationPipelineBuilder` against filters in `.eval.pipeline` | `src/euc/grantfitassessment` |
| `grant-fit-assessment.json` | The EUC itself (goal, rules, policies, execution requirements, evaluation criteria, each criterion's `tracesTo`) | `resources/euc/grant-fit-assessment` |
| Eval dataset | Test organizations with independently-established ground truth | `eval/grant-fit-assessment/dataset` |
| Eval results | Output of evaluation runs (drift detection, false-flag rate, etc.) | `eval/grant-fit-assessment/results` |

## Status

- [x] EUC schema defined and JSON authored
- [x] EUC model + loader (Python), `validate()` enforcing the structural contract at load time — every execution requirement has an id and a type, every evaluation criterion traces to a declared requirement, rule, or policy
- [x] Deterministic eligibility rule engine (`euc.grantfitassessment.eligibility.check_eligibility`, unit tested)
- [x] LLM-reasoned fit assessment (`LlmFitReasoner`, calls the Anthropic Messages API directly — requires `ANTHROPIC_API_KEY`)
- [x] Evaluator bound to the EUC's evaluation criteria (`GrantFitEvaluator`, unit tested)
- [x] Eval dataset with independently-established ground truth — 6 cases in `eval/grant-fit-assessment/dataset/test-cases.json`, including both edge cases from the case study (eligible-but-misaligned, ineligible-but-aligned)
- [x] `PipelineBuilder` compiles the EUC's `executionRequirements` into a LangGraph `StateGraph`, in declared order: each node resolves the requirement's id through an `ExecutionFilterRegistry` to its filter, and a `FAILED` outcome on a requirement whose `onFailure` is `halt` routes straight to the graph's end instead of the next node — verified offline in `test_grant_fit_application.py` (a fake `FitReasoner` asserts if it's ever invoked after a halt) and `test_pipeline_builder.py` (ordering/halt/continue against synthetic requirements)
- [x] `EvaluationPipelineBuilder` runs the EUC's `evaluationCriteria` the same way, but as a plain ordered loop — every criterion is scored regardless of another's verdict, since each measures something independent rather than gating a shared outcome
- [x] `tracesTo` is enforced at load time: a criterion tracing to an undeclared id fails validation naming the broken link; `untraced_ids()` reports anything no criterion checks, so mapping gaps are visible rather than silent
- [x] Drift-experiment harness (`docs/proposal.md` Section 7): `FitReasonerVariant` (a `FitReasoner` + a declared "expected to alter behavior" flag), `DriftExperimentRunner` (runs the dataset against a baseline + candidate variants and computes the four Section 7 metrics), `DriftExperimentReport`/`drift_experiment_report_writer` (summary + JSON output to `eval/grant-fit-assessment/results/`), and `AlternateAlignmentPromptReasoner` as a ready-made prompt-variant example (`LlmFitReasoner._alignment_instructions()` is the documented override point for prompt variants) — verified offline in `test_drift_experiment_runner.py` with fake reasoners, and live against Claude
- [x] 38 tests passing offline, covering the deterministic layer, the halt contract, the evaluators, the controlled-change metrics, the traceability contract, and the graph-based orchestration engine
- [x] Live run against Claude verified for the application, the evaluation runner, and the drift experiment

## Build & Run

Requirements: Python 3.11+

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"

# Run the test suite (deterministic layer — no API key needed)
pytest

# Run the Grant Fit application against the sample EUC (requires ANTHROPIC_API_KEY)
python -m euc.grantfitassessment.app

# Run the evaluation suite against eval/grant-fit-assessment/dataset/test-cases.json (requires ANTHROPIC_API_KEY)
python -m euc.grantfitassessment.eval.evaluation_runner

# Run the drift experiment: baseline vs. prompt/model variants (requires ANTHROPIC_API_KEY)
# optional — set LLM_MODEL_VARIANT to also test a model swap alongside the built-in prompt variant
python -m euc.grantfitassessment.eval.drift_experiment_main
```

`LlmFitReasoner` calls the Anthropic Messages API directly. Set your key via
environment variable before running the app or the eval suite:

```bash
export ANTHROPIC_API_KEY=your-anthropic-api-key
# optional — defaults to claude-sonnet-4-6
export LLM_MODEL=claude-sonnet-4-6
```

Never commit a real key or paste it into a chat session — see `.env.example`
for the variables this project reads, and set them via your shell (session
export or shell profile) rather than a tracked file.

## Repository Structure

Each EUC gets its own folder under `src/euc/`, `resources/euc/`, and `eval/` —
`euc.core` is the only domain-agnostic piece, shared by every EUC.

```
.
├── README.md
├── LICENSE
├── pyproject.toml
├── docs/
│   └── proposal.md                # Full design rationale, falsifiable claims, eval methodology
├── src/euc/
│   ├── core/                      # Domain-agnostic engine: EUC model, loader, both pipeline builders
│   └── grantfitassessment/        # Grant Fit Assessment EUC — application, evaluator, eval/drift runners
│       ├── pipeline/              #   execution filters (eligibility, geography, requiredInfo, alignment)
│       └── eval/
│           └── pipeline/          #   evaluation filters (eligibilityCorrectness, programAlignment, evidenceGrounding)
├── resources/euc/
│   └── grant-fit-assessment/
│       └── grant-fit-assessment.json
├── tests/
│   ├── core/                      # Engine unit tests
│   └── grantfitassessment/        # Grant Fit Assessment unit + offline integration tests
│       └── eval/
└── eval/
    └── grant-fit-assessment/
        ├── dataset/                # Test cases with ground truth
        └── results/                # Evaluation/drift-experiment run outputs (populated by the runners)
```
