package com.euc.grantfitassessment.web;

import com.euc.core.EucDefinition;
import com.euc.grantfitassessment.AlternateAlignmentPromptReasoner;
import com.euc.grantfitassessment.LlmFitReasoner;
import com.euc.grantfitassessment.eval.DriftExperimentReport;
import com.euc.grantfitassessment.eval.DriftExperimentReportWriter;
import com.euc.grantfitassessment.eval.DriftExperimentRunner;
import com.euc.grantfitassessment.eval.FitReasonerVariant;
import com.euc.grantfitassessment.eval.TestCase;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * POST /api/drift-experiment — runs the Week 5 drift experiment (baseline
 * LlmFitReasoner vs. AlternateAlignmentPromptReasoner, plus a model-swap
 * variant if LLM_MODEL_VARIANT is set) against the eval dataset and
 * returns the DriftExperimentReport as JSON — same variants as
 * DriftExperimentMain, but returned over HTTP instead of written to
 * eval/grant-fit-assessment/results/, so repeated clicks from the UI don't
 * accumulate files. Makes several live LLM calls (one per case per
 * variant, including the baseline) — the slowest endpoint in the API.
 */
class DriftExperimentHandler implements HttpHandler {

    private final EucDefinition euc;
    private final List<TestCase> dataset;

    DriftExperimentHandler(EucDefinition euc, List<TestCase> dataset) {
        this.euc = euc;
        this.dataset = dataset;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonHttpUtil.sendMethodNotAllowed(exchange, "POST");
            return;
        }

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

        try {
            DriftExperimentRunner runner = new DriftExperimentRunner(euc, dataset);
            DriftExperimentReport report = runner.run(baseline, candidates);
            ObjectNode json = DriftExperimentReportWriter.toJson(report);
            JsonHttpUtil.sendJson(exchange, 200, json);
        } catch (RuntimeException e) {
            JsonHttpUtil.sendError(exchange, 502, e.getMessage());
        }
    }
}
