package com.euc.grantfitassessment.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Serializes a DriftExperimentReport to JSON (eval/grant-fit-assessment/results/,
 * per the README's repository layout, and the REST API's /api/drift-experiment
 * response — see com.euc.grantfitassessment.web). Writes data only, not
 * FitReasoner instances — variants are identified by their label, not by
 * trying to serialize the reasoner object behind them.
 */
public class DriftExperimentReportWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void writeJson(DriftExperimentReport report, File outputFile) {
        ObjectNode root = toJson(report);

        try {
            File parent = outputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(outputFile, root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write drift experiment report to " + outputFile, e);
        }
    }

    public static ObjectNode toJson(DriftExperimentReport report) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("baseline", report.baseline().label());
        root.put("driftDetectionRate", nanSafe(report.driftDetectionRate()));
        root.put("falseFlagRate", nanSafe(report.falseFlagRate()));
        root.put("deterministicRuleStabilityRate", nanSafe(report.deterministicRuleStabilityRate()));
        root.put("evidenceGroundingConsistencyRate", nanSafe(report.evidenceGroundingConsistencyRate()));

        ArrayNode variants = root.putArray("variants");
        for (DriftExperimentRunner.VariantResult result : report.variantResults()) {
            ObjectNode variantNode = variants.addObject();
            variantNode.put("label", result.variant().label());
            variantNode.put("expectedToAlterBehavior", result.variant().expectedToAlterBehavior());
            variantNode.put("anyDriftFlagged", result.anyDriftFlagged());
            variantNode.put("deterministicRuleStable", result.deterministicRuleStable());
            variantNode.put("evidenceGroundingRate", nanSafe(result.evidenceGroundingRate()));

            ArrayNode flagged = variantNode.putArray("flaggedCaseIds");
            result.flaggedCaseIds().forEach(flagged::add);

            ArrayNode driftCases = variantNode.putArray("eligibilityCorrectnessDriftCaseIds");
            result.eligibilityCorrectnessDriftCaseIds().forEach(driftCases::add);

            ObjectNode casesNode = variantNode.putObject("cases");
            for (Map.Entry<String, DriftExperimentRunner.CaseOutcome> entry : result.outcomes().entrySet()) {
                ObjectNode caseNode = casesNode.putObject(entry.getKey());
                DriftExperimentRunner.CaseOutcome outcome = entry.getValue();
                caseNode.put("eligible", outcome.actual().eligible());
                caseNode.put("fitClassification", outcome.actual().fitClassification());
                caseNode.put("eligibilityCorrectness", outcome.score().eligibilityCorrectness());
                caseNode.put("programAlignment", outcome.score().programAlignment());
                caseNode.put("evidenceGrounding", outcome.score().evidenceGrounding());
                caseNode.put("allPassed", outcome.score().allPassed());
            }
        }

        return root;
    }

    private static Double nanSafe(double value) {
        return Double.isNaN(value) ? null : value;
    }
}
