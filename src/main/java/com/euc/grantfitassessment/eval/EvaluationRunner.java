package com.euc.grantfitassessment.eval;

import com.euc.core.EucDefinition;
import com.euc.core.EucLoader;
import com.euc.grantfitassessment.AssessmentResult;
import com.euc.grantfitassessment.FitReasoner;
import com.euc.grantfitassessment.GrantFitApplication;
import com.euc.grantfitassessment.LlmFitReasoner;

import java.io.File;
import java.util.List;

/**
 * Runs the eval dataset (eval/grant-fit-assessment/dataset/test-cases.json)
 * against a GrantFitApplication instance and reports per-criterion pass
 * rates, per docs/proposal.md Section 6.
 *
 * Run from the project root so the relative path to eval/grant-fit-assessment
 * resolves:
 *   mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.eval.EvaluationRunner"
 *
 * This is a single baseline pass — one FitReasoner, one pass/fail count.
 * For the Week 5 drift experiment (Section 7): running the same dataset
 * across multiple FitReasoner variants and computing drift-detection-rate /
 * false-flag-rate / deterministic-rule-stability / evidence-grounding-
 * consistency, see DriftExperimentRunner and DriftExperimentMain instead.
 */
public class EvaluationRunner {

    private static final String DATASET_PATH = "eval/grant-fit-assessment/dataset/test-cases.json";

    public static void main(String[] args) {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        String modelName = System.getenv().getOrDefault("LLM_MODEL", "claude-sonnet-4-6");
        FitReasoner reasoner = new LlmFitReasoner(modelName);
        GrantFitApplication app = new GrantFitApplication(euc, reasoner);
        GrantFitEvaluator evaluator = new GrantFitEvaluator(euc);

        List<TestCase> testCases = new TestCaseDataset().loadFromFile(new File(DATASET_PATH));

        int passed = 0;
        for (TestCase tc : testCases) {
            try {
                AssessmentResult result = app.assess(tc.organization(), tc.grant());
                GrantFitEvaluator.EvaluationScore score = evaluator.evaluate(tc, result);
                System.out.println(tc.caseId() + " -> " + score);
                if (score.allPassed()) {
                    passed++;
                }
            } catch (UnsupportedOperationException e) {
                // Deterministic-only cases (eligibility failures) never reach the
                // reasoner, so they still evaluate cleanly even before
                // LlmFitReasoner is fully wired to a live model call.
                System.out.println(tc.caseId() + " -> reasoning not yet available: " + e.getMessage());
            }
        }

        System.out.printf("%n%d / %d test cases passed all criteria%n", passed, testCases.size());
    }
}
