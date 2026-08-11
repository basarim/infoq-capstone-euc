package com.euc.grantfit.eval;

import com.euc.core.EucDefinition;
import com.euc.core.EucLoader;
import com.euc.grantfit.AssessmentResult;
import com.euc.grantfit.EligibilityChecker;
import com.euc.grantfit.FitReasoner;
import com.euc.grantfit.GrantFitApplication;
import com.euc.grantfit.LlmFitReasoner;

import java.io.File;
import java.util.List;

/**
 * Runs the eval dataset (eval/dataset/test-cases.json) against a
 * GrantFitApplication instance and reports per-criterion pass rates, per
 * docs/proposal.md Section 6.
 *
 * Run from the project root so the relative path to eval/dataset resolves:
 *   mvn exec:java -Dexec.mainClass="com.euc.grantfit.eval.EvaluationRunner"
 *
 * TODO (Week 5): extend this to run the same dataset across multiple
 * FitReasoner implementations (model/prompt variants) and compute the
 * drift detection rate / false-flag rate metrics from Section 6, rather
 * than a single pass/fail count.
 */
public class EvaluationRunner {

    private static final String DATASET_PATH = "eval/dataset/test-cases.json";

    public static void main(String[] args) {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();

        String modelName = System.getenv().getOrDefault("LLM_MODEL", "claude-sonnet-4-6");
        FitReasoner reasoner = new LlmFitReasoner(modelName);
        GrantFitApplication app = new GrantFitApplication(euc, new EligibilityChecker(), reasoner);
        GrantFitEvaluator evaluator = new GrantFitEvaluator();

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
