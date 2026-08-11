package com.euc.grantfit.eval;

import com.euc.grantfit.AssessmentResult;
import com.euc.grantfit.GrantOpportunity;
import com.euc.grantfit.Organization;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantFitEvaluatorTest {

    private final GrantFitEvaluator evaluator = new GrantFitEvaluator();

    private TestCase sampleTestCase(String expectedFit, List<String> expectedKeywords) {
        Organization org = new Organization("Org", "Mission", List.of("Program"), "PNW", true);
        GrantOpportunity grant = new GrantOpportunity(
                "Funder", "Grant", List.of("Priority"), List.of(), List.of("PNW"), true);
        return new TestCase("case-1", org, grant, true, expectedFit, "rationale", expectedKeywords);
    }

    @Test
    void matchingResultPassesAllCriteria() {
        TestCase tc = sampleTestCase("STRONG_FIT", List.of("STEM"));
        AssessmentResult actual = new AssessmentResult(
                true, List.of(), "STRONG_FIT", "explanation",
                List.of("Strong STEM alignment"), List.of());

        GrantFitEvaluator.EvaluationScore score = evaluator.evaluate(tc, actual);
        assertTrue(score.allPassed());
    }

    @Test
    void mismatchedClassificationFailsProgramAlignment() {
        TestCase tc = sampleTestCase("STRONG_FIT", List.of());
        AssessmentResult actual = new AssessmentResult(
                true, List.of(), "POOR_FIT", "explanation", List.of(), List.of());

        GrantFitEvaluator.EvaluationScore score = evaluator.evaluate(tc, actual);
        assertFalse(score.programAlignment());
        assertFalse(score.allPassed());
    }

    @Test
    void missingExpectedEvidenceFailsEvidenceGrounding() {
        TestCase tc = sampleTestCase("STRONG_FIT", List.of("STEM", "underserved"));
        AssessmentResult actual = new AssessmentResult(
                true, List.of(), "STRONG_FIT", "explanation",
                List.of("Some unrelated evidence"), List.of());

        GrantFitEvaluator.EvaluationScore score = evaluator.evaluate(tc, actual);
        assertFalse(score.evidenceGrounding());
        assertTrue(score.missingEvidenceKeywords().containsAll(List.of("STEM", "underserved")));
    }

    @Test
    void eligibilityMismatchFailsEligibilityCorrectness() {
        TestCase tc = sampleTestCase("STRONG_FIT", List.of());
        AssessmentResult actual = new AssessmentResult(
                false, List.of("ELIGIBILITY-001"), "POOR_FIT",
                "not eligible", List.of(), List.of());

        GrantFitEvaluator.EvaluationScore score = evaluator.evaluate(tc, actual);
        assertFalse(score.eligibilityCorrectness());
    }
}
