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

  euc/grant-fit-assessment.json   <- single source of truth
       |                                |
       v                                v
  GrantFitApplication              GrantFitEvaluator
  (deterministic rules             (scores reasoned result
   + LLM reasoning)                 against EUC criteria)
```

| Component | Responsibility | Location |
|---|---|---|
| `com.euc.core` | EUC model + loader — parses the EUC JSON into typed Java objects | `src/main/java/com/euc/core` |
| `com.euc.grantfit` | Grant Fit domain logic — deterministic eligibility rules + LLM-reasoned fit assessment | `src/main/java/com/euc/grantfit` |
| `euc/grant-fit-assessment.json` | The EUC itself (goal, rules, policies, expected outcomes, evaluation criteria) | `src/main/resources/euc` |
| Eval dataset | Test organizations with independently-established ground truth | `eval/dataset` |
| Eval results | Output of evaluation runs (drift detection, false-flag rate, etc.) | `eval/results` |

## Status (Week 4 — Working Prototype)

- [x] EUC schema defined and JSON authored
- [x] EUC model + loader (Java)
- [x] Deterministic eligibility rule engine (`EligibilityChecker`, unit tested)
- [x] LLM-reasoned fit assessment (`LlmFitReasoner`, calls Anthropic Messages API — requires `LLM_API_KEY`)
- [x] Evaluator bound to EUC evaluation criteria (`GrantFitEvaluator`, unit tested)
- [x] Eval dataset with ground truth — 6 cases in `eval/dataset/test-cases.json`, including both edge cases from the case study (eligible-but-misaligned, ineligible-but-aligned)
- [x] Dataset loader (`TestCaseDataset`) wired into `EvaluationRunner`
- [ ] First live eval pass run against a real model, and gaps documented in "Lessons Learned" below
- [ ] Drift-detection experiment across model/prompt variants (Section 6 of `docs/proposal.md`) — planned for Week 5

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

# Run the evaluation suite against eval/dataset/test-cases.json (requires LLM_API_KEY)
mvn exec:java -Dexec.mainClass="com.euc.grantfit.eval.EvaluationRunner"
```

`LlmFitReasoner` calls the Anthropic Messages API directly. Set your key via
environment variable before running the app or the eval suite:

```bash
export LLM_API_KEY=your-anthropic-api-key
# optional — defaults to claude-sonnet-4-6
export LLM_MODEL=claude-sonnet-4-6
```

## Lessons Learned

_To be completed after the first end-to-end eval pass — see Week 4/5 milestones._

## Repository Structure

```
.
├── README.md
├── LICENSE
├── pom.xml
├── docs/
│   └── proposal.md          # Full design rationale, falsifiable claims, eval methodology
├── src/
│   ├── main/java/com/euc/
│   │   ├── core/             # EUC model + loader
│   │   └── grantfit/         # Grant Fit application, evaluator, eval runner
│   ├── main/resources/euc/
│   │   └── grant-fit-assessment.json
│   └── test/java/com/euc/    # Unit tests (deterministic layer — no API key needed)
└── eval/
    ├── dataset/              # Test cases with ground truth
    └── results/              # Evaluation run outputs (populated by EvaluationRunner)
```
