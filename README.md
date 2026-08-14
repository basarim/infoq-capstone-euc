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
that capture business intent (goals, rules, policies, expected outcomes, and evaluation
criteria) as a single definition consumed by *both* the running application and its
evaluation suite, rather than two definitions maintained independently.

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

`com.euc.core` is a domain-agnostic engine (EUC model, loader, and both
`PipelineBuilder`s); everything specific to the Grant Fit use case — code,
resource, dataset, and results — lives under one folder per EUC, so a second
EUC can be added later as a sibling package/folder without touching the
engine.

| Component | Responsibility | Location |
|---|---|---|
| `com.euc.core` | EUC model + loader, plus both pipeline builders (execution and evaluation) that assemble a filter chain mechanically from the EUC JSON (see [`docs/proposal.md`](docs/proposal.md) Section 4) | `src/main/java/com/euc/core` |
| `com.euc.grantfitassessment` | Grant Fit domain logic — deterministic eligibility checks + LLM-reasoned fit assessment, executed via `PipelineBuilder` reading `executionPipeline` (filters in `.pipeline`); evaluation scored via `EvaluationPipelineBuilder` reading `evaluationPipeline` (filters in `.eval.pipeline`); see Status below | `src/main/java/com/euc/grantfitassessment` |
| `grant-fit-assessment.json` | The EUC itself (goal, rules, policies, expected outcomes, evaluation criteria) | `src/main/resources/euc/grant-fit-assessment` |
| Eval dataset | Test organizations with independently-established ground truth | `eval/grant-fit-assessment/dataset` |
| Eval results | Output of evaluation runs (drift detection, false-flag rate, etc.) | `eval/grant-fit-assessment/results` |

## Status (Week 4 — Working Prototype)

- [x] EUC schema defined and JSON authored
- [x] EUC model + loader (Java)
- [x] Deterministic eligibility rule engine (`EligibilityChecker`, unit tested)
- [x] LLM-reasoned fit assessment (`LlmFitReasoner`, calls Anthropic Messages API — requires `LLM_API_KEY`)
- [x] Evaluator bound to EUC evaluation criteria (`GrantFitEvaluator`, unit tested)
- [x] Eval dataset with ground truth — 6 cases in `eval/grant-fit-assessment/dataset/test-cases.json`, including both edge cases from the case study (eligible-but-misaligned, ineligible-but-aligned)
- [x] Dataset loader (`TestCaseDataset`) wired into `EvaluationRunner`
- [x] `EucDefinition.validate()` enforces the pipeline contract — one or more filters per pipeline, every stage has a filter key (called automatically by `EucLoader`)
- [x] `PipelineBuilder` assembles the execution chain from `executionPipeline`: each stage's `filter` key ("eligibility", "geography", "requiredInfo", "alignmentReasoning") resolves through an `ExecutionFilterRegistry` to a filter class in `com.euc.grantfitassessment.pipeline`; `GrantFitApplication` builds the registry and runs the pipeline instead of hand-wiring the checks. `onFailure: halt` stops the pipeline before `ALIGNMENT-001` runs — verified offline in `GrantFitApplicationTest` using a fake `FitReasoner` that asserts if it's ever invoked after a halt.
- [x] `EvaluationPipelineBuilder` assembles the evaluation chain from `evaluationPipeline` the same way: each stage's `filter` key ("eligibilityCorrectness", "programAlignment", "evidenceGrounding") resolves through an `EvaluationFilterRegistry` to a filter class in `com.euc.grantfitassessment.eval.pipeline`; `GrantFitEvaluator`'s public API is unchanged, but it now scores by running the pipeline rather than hand-checking the three criteria — both halves of the EUC are schema-driven.
- [x] Fixed a bug found while wiring the above: `EucLoader`'s `ObjectMapper` didn't enable case-insensitive enum matching, so the EUC JSON (`"type": "deterministic"`, `"onFailure": "halt"`) never actually deserialized against the uppercase Java enums — every `EucLoader.loadGrantFitAssessment()` call was failing before this fix, including in the existing test suite.
- [x] Drift-experiment scaffolding for Week 5 (Section 7 of `docs/proposal.md`): `FitReasonerVariant` (a `FitReasoner` + a declared "expected to alter behavior" flag), `DriftExperimentRunner` (runs the dataset against a baseline + candidate variants and computes the four Section 7 metrics), `DriftExperimentReport`/`DriftExperimentReportWriter` (summary + JSON output to `eval/grant-fit-assessment/results/`), and `AlternateAlignmentPromptReasoner` as a ready-made prompt-variant example (`LlmFitReasoner.alignmentInstructions()` is now the documented override point for prompt variants). Verified offline in `DriftExperimentRunnerTest` with fake reasoners — all four metrics computed correctly against known inputs.
- [ ] First live eval pass and first live drift-experiment run against a real model (`LLM_API_KEY` not available in this environment — all wiring is verified offline; only the network call to Anthropic itself remains unverified), and gaps documented in "Lessons Learned" below

**Not yet done, by design:** the reasoning layer has not been run against
a live model in this environment (no API key configured here). Running it
is the natural next step once you have this repo locally — see Build & Run.

## Build & Run

Requirements: Java 17+, Maven 3.8+

```bash
# Build
mvn clean install

# Run unit tests only (deterministic layer — no API key needed)
mvn test

# Run the Grant Fit application against the sample EUC (requires LLM_API_KEY)
mvn exec:java

# Run the evaluation suite against eval/grant-fit-assessment/dataset/test-cases.json (requires LLM_API_KEY)
mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.eval.EvaluationRunner"

# Run the Week 5 drift experiment: baseline vs. prompt/model variants (requires LLM_API_KEY)
# optional — set LLM_MODEL_VARIANT to also test a model swap alongside the built-in prompt variant
mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.eval.DriftExperimentMain"
```

`LlmFitReasoner` calls the Anthropic Messages API directly. Set your key via
environment variable before running the app or the eval suite:

```bash
export LLM_API_KEY=your-anthropic-api-key
# optional — defaults to claude-sonnet-4-6
export LLM_MODEL=claude-sonnet-4-6
```

Never commit a real key or paste it into a chat session — see `.env.example`
for the two variables this project reads, and set them via your shell
(session export or shell profile) rather than a tracked file.

## Lessons Learned

Both the execution and evaluation pipelines are now verified offline
(`GrantFitApplicationTest`, `DriftExperimentRunnerTest` — no API key
needed) — the remaining gap for a first live pass is purely the Anthropic
network call and reading a real model's output through
`LlmFitReasoner.parseResponse`. Full writeup pending after that first
end-to-end run — see Week 4/5 milestones.

## Repository Structure

Each EUC gets its own folder under `src/main/java/com/euc/`,
`src/main/resources/euc/`, and `eval/` — `com.euc.core` is the only
domain-agnostic piece, shared by every EUC.

```
.
├── README.md
├── LICENSE
├── pom.xml
├── docs/
│   └── proposal.md                    # Full design rationale, falsifiable claims, eval methodology
├── src/
│   ├── main/java/com/euc/
│   │   ├── core/                      # Domain-agnostic engine: EUC model, loader, both PipelineBuilders
│   │   └── grantfitassessment/        # Grant Fit Assessment EUC — application, evaluator, eval/drift runners
│   │       ├── pipeline/              #   execution filters (eligibility, geography, requiredInfo, alignmentReasoning)
│   │       └── eval/
│   │           └── pipeline/          #   evaluation filters (eligibilityCorrectness, programAlignment, evidenceGrounding)
│   ├── main/resources/euc/
│   │   └── grant-fit-assessment/
│   │       └── grant-fit-assessment.json
│   └── test/java/com/euc/
│       ├── core/                      # Engine unit tests
│       └── grantfitassessment/        # Grant Fit Assessment unit + offline integration tests
│           └── eval/
└── eval/
    └── grant-fit-assessment/
        ├── dataset/                   # Test cases with ground truth
        └── results/                   # Evaluation/drift-experiment run outputs (populated by the runners)
```
