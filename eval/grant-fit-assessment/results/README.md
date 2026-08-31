# Eval Results — Grant Fit Assessment

Populated by running the eval suite or the drift experiment — nothing here
is checked in as static data, since a committed "result" would go stale the
moment the model or prompt changes underneath it.

- `mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.eval.EvaluationRunner"` —
  single baseline pass against `eval/grant-fit-assessment/dataset/test-cases.json`,
  printed to stdout only.
- `mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.eval.DriftExperimentMain"` —
  the Week 5 drift experiment (docs/proposal.md Section 7): baseline vs.
  variant `FitReasoner`s, scored against the same dataset. Writes a
  timestamped JSON report here (`drift-experiment-<timestamp>.json`) and
  prints a summary.

Both require `ANTHROPIC_API_KEY` — see the root README's Build & Run section.
