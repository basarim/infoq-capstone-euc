package com.euc.grantfitassessment.eval;

import com.euc.core.EucDefinition;
import com.euc.core.EucLoader;
import com.euc.grantfitassessment.AlternateAlignmentPromptReasoner;
import com.euc.grantfitassessment.LlmFitReasoner;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the Week 5 drift experiment (docs/proposal.md Section 7) against a
 * live model: a baseline LlmFitReasoner vs. a prompt-variant reasoner
 * (AlternateAlignmentPromptReasoner, expected to alter behavior) and,
 * optionally, a model-swap variant if LLM_MODEL_VARIANT is set. Requires
 * ANTHROPIC_API_KEY — see README Build & Run.
 *
 * Run from the project root:
 *   mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.eval.DriftExperimentMain"
 *
 * Writes a JSON report to eval/grant-fit-assessment/results/ and prints a
 * summary. See DriftExperimentRunnerTest for an offline proof of the same
 * mechanics using a fake FitReasoner (no API key needed).
 */
public class DriftExperimentMain {

    private static final String DATASET_PATH = "eval/grant-fit-assessment/dataset/test-cases.json";
    private static final String RESULTS_DIR = "eval/grant-fit-assessment/results";

    public static void main(String[] args) {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();
        List<TestCase> dataset = new TestCaseDataset().loadFromFile(new File(DATASET_PATH));

        String baselineModel = System.getenv().getOrDefault("LLM_MODEL", "claude-sonnet-4-6");
        FitReasonerVariant baseline = new FitReasonerVariant(
                "baseline:" + baselineModel, new LlmFitReasoner(baselineModel), false);

        List<FitReasonerVariant> candidates = new ArrayList<>();
        candidates.add(new FitReasonerVariant(
                "prompt-variant:loosened-alignment-instructions",
                new AlternateAlignmentPromptReasoner(baselineModel),
                true));

        String alternateModel = System.getenv("LLM_MODEL_VARIANT");
        if (alternateModel != null && !alternateModel.isBlank()) {
            candidates.add(new FitReasonerVariant(
                    "model-variant:" + alternateModel, new LlmFitReasoner(alternateModel), true));
        }

        DriftExperimentRunner runner = new DriftExperimentRunner(euc, dataset);
        DriftExperimentReport report = runner.run(baseline, candidates);

        System.out.println(report.summary());

        File outputFile = new File(RESULTS_DIR + "/drift-experiment-"
                + Instant.now().toString().replace(":", "-") + ".json");
        DriftExperimentReportWriter.writeJson(report, outputFile);
        System.out.println("Wrote " + outputFile);
    }
}
