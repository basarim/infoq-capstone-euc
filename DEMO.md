# Demo Script — Executable Use Cases: Grant Fit Assessment

A ~8-10 minute walkthrough script for presenting the prototype. Pair with
`./demo.sh`, which builds, verifies, and opens the web UI automatically.

## Before you start

```bash
export LLM_API_KEY="sk-ant-..."   # in your own shell, not committed anywhere
./demo.sh
```

This builds the project, runs the 30 offline tests, starts the web UI, and
opens it in your browser. If `LLM_API_KEY` isn't set, `demo.sh` stops with a
clear message before wasting anyone's time — see **Fallback plan** below if
you're demoing without a working key.

---

## 1. The claim, in one breath (30s)

> "AI models drift — prompts get tweaked, models get swapped — but the
> business rule for 'is this a good grant fit' shouldn't have to be
> rewritten every time. This project tests whether you can define that
> rule *once*, as data, and have both the running application and its own
> evaluation suite read the same definition — so drift becomes something
> you can detect, not something you have to hope didn't happen."

## 2. The EUC itself (1-2 min)

In the browser, point at the **"Loaded EUC"** section — or open
`/api/euc` directly in a second tab.

Say: *"This JSON file is the single source of truth. Nothing here is
duplicated between the app and its evaluator."*

Point out on screen:
- **Execution pipeline** — 4 stages, in order: `eligibility`, `geography`,
  `requiredInfo` (all deterministic, all `onFailure: halt`), then
  `alignmentReasoning` (the one LLM call).
- **Evaluation pipeline** — 3 criteria (`eligibilityCorrectness`,
  `programAlignment`, `evidenceGrounding`), each naming which execution
  stage it checks.

*(If asked "where does this live in code" — `src/main/resources/euc/grant-fit-assessment/grant-fit-assessment.json`,
loaded once by `EucLoader`, consumed by both `PipelineBuilder` (execution)
and `EvaluationPipelineBuilder` (evaluation) — same file, two consumers.)*

## 3. Try an assessment — happy path (2 min)

- Pick **strong-fit-01** from the sample dropdown, click **Run Assessment**.
- While it's running: *"This just walked all four pipeline stages —
  eligibility, geography, required info all passed deterministically, then
  it called the LLM for the one stage that actually needs judgment:
  mission alignment."*
- Show the result: `eligible: true`, `STRONG_FIT`, explanation, supporting
  evidence.

## 4. Try an assessment — the halt path (1-2 min)

- Pick **ineligible-not-nonprofit-01** (or type in an org with the
  "registered nonprofit" box unchecked), run it.
- Point out: `POOR_FIT`, no supporting evidence, and — the important
  part — **no LLM call happened at all**. The pipeline halted at
  `ELIGIBILITY-001` per its `onFailure: halt` policy. Strong alignment
  can't overcome a failed mandatory rule; the code enforces that
  structurally, not by convention.

## 5. Run Full Evaluation (1 min)

Click **Run Full Evaluation**. This scores all 6 ground-truth test cases —
including the two deliberately tricky edge cases (eligible-but-misaligned,
ineligible-but-well-aligned) — against the EUC's own evaluation criteria.

*"This is the same evaluator the drift experiment uses — it's not a
separate test suite that happens to check similar things."*

## 6. Run Drift Experiment — Week 5 (2 min)

Click **Run Drift Experiment**. While it runs (several LLM calls, takes a
bit): explain the setup — a baseline reasoner vs. a prompt-variant
reasoner with deliberately loosened alignment instructions, both scored
against the *same unchanged EUC*.

When it finishes, walk the four metrics:
- **drift-detection-rate** — did evaluation catch the variant that was
  expected to behave differently?
- **false-flag-rate** — n/a here (no behavior-neutral variant in this
  run) unless `LLM_MODEL_VARIANT` was also set.
- **deterministic-rule-stability-rate** — should be 100%: eligibility
  logic never touches the reasoner, so it must never move.
- **evidence-grounding-consistency-rate** — how often the variant's
  cited evidence still held up against ground truth.

*"This is the actual falsifiable claim the whole project is testing —
Section 5 of the proposal. A high detection rate and stable deterministic
rules would support it; a miss here would be a genuine, informative
negative result, not a bug to paper over."*

## 7. Wrap-up (30s)

- Every piece of this is schema-driven: add a stage to the EUC JSON,
  register a filter, and it runs — no pipeline code to touch.
- `com.euc.core` is domain-agnostic; Grant Fit Assessment is one EUC
  under `com.euc.grantfitassessment` — a second use case would be a
  sibling folder, not a rewrite.

---

## Fallback plan (no working `LLM_API_KEY` at demo time)

Everything above except the actual LLM call is provable without a key:

```bash
mvn clean install    # 30/30 tests pass offline — no key needed
```

Walk through `GrantFitApplicationTest` instead — it proves the exact same
pipeline mechanics (execution order, the eligibility halt, the evaluator
scoring) using a fake reasoner, so you can show the wiring is correct even
without a live model call. The web UI will still load, show the EUC, and
run the deterministic-only sample cases (any ineligible one) successfully
— only the LLM-backed stage will 502.

## Anticipated questions

- **"What if the model changes and evaluation doesn't catch it?"** — That's
  exactly `claim-1-primary`'s false-negative risk (proposal §5). This
  project's real contribution is having built infrastructure that can
  actually measure that, not an assumption that it won't happen.
- **"Why not just write more unit tests?"** — Unit tests check code
  against a human's expectation, written once and rarely revisited.
  This checks the *running application's output* against the *same*
  business-intent definition the application itself reads, every run —
  the point is the definition can't silently drift out of sync with what
  it's supposed to validate, because there's only one copy of it.
- **"Has this actually caught real drift yet?"** — Be honest here: the
  offline mechanics are fully verified; a live end-to-end run is the
  next concrete step (see README "Lessons Learned"). This is a legitimate
  place to be at this stage of the project, not a gap to hide.
