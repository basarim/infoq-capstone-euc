package com.euc.grantfitassessment.web;

import com.euc.grantfitassessment.AssessmentResult;
import com.euc.grantfitassessment.GrantFitApplication;
import com.euc.grantfitassessment.eval.GrantFitEvaluator;
import com.euc.grantfitassessment.eval.TestCase;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * POST /api/evaluate — runs the full eval dataset (ground truth) against
 * the application and returns per-case scores, mirroring EvaluationRunner
 * but as JSON for the web UI. Makes one live LLM call per eligible test
 * case (LLM_API_KEY required); a single case's failure doesn't abort the
 * batch — it's reported inline so the UI can show partial results.
 */
class EvaluateHandler implements HttpHandler {

    private final GrantFitApplication app;
    private final GrantFitEvaluator evaluator;
    private final List<TestCase> dataset;

    EvaluateHandler(GrantFitApplication app, GrantFitEvaluator evaluator, List<TestCase> dataset) {
        this.app = app;
        this.evaluator = evaluator;
        this.dataset = dataset;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonHttpUtil.sendMethodNotAllowed(exchange, "POST");
            return;
        }

        ObjectNode root = JsonHttpUtil.MAPPER.createObjectNode();
        ArrayNode results = root.putArray("results");
        int passed = 0;

        for (TestCase tc : dataset) {
            ObjectNode caseNode = results.addObject();
            caseNode.put("caseId", tc.caseId());
            try {
                AssessmentResult actual = app.assess(tc.organization(), tc.grant());
                GrantFitEvaluator.EvaluationScore score = evaluator.evaluate(tc, actual);
                caseNode.put("eligible", actual.eligible());
                caseNode.put("fitClassification", actual.fitClassification());
                caseNode.put("eligibilityCorrectness", score.eligibilityCorrectness());
                caseNode.put("programAlignment", score.programAlignment());
                caseNode.put("evidenceGrounding", score.evidenceGrounding());
                caseNode.put("allPassed", score.allPassed());
                ArrayNode missing = caseNode.putArray("missingEvidenceKeywords");
                score.missingEvidenceKeywords().forEach(missing::add);
                if (score.allPassed()) {
                    passed++;
                }
            } catch (RuntimeException e) {
                caseNode.put("error", e.getMessage());
            }
        }

        root.put("passed", passed);
        root.put("total", dataset.size());
        JsonHttpUtil.sendJson(exchange, 200, root);
    }
}
