package com.euc.grantfitassessment.web;

import com.euc.core.EucDefinition;
import com.euc.core.EucLoader;
import com.euc.grantfitassessment.GrantFitApplication;
import com.euc.grantfitassessment.LlmFitReasoner;
import com.euc.grantfitassessment.eval.GrantFitEvaluator;
import com.euc.grantfitassessment.eval.TestCase;
import com.euc.grantfitassessment.eval.TestCaseDataset;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server exposing a REST API + a static experimentation UI
 * over the Grant Fit Assessment EUC — lets you try assessments, run the
 * eval dataset, and run the Week 5 drift experiment from a browser instead
 * of the CLI entry points (GrantFitApplication, EvaluationRunner,
 * DriftExperimentMain), which this reuses directly rather than
 * duplicating their logic.
 *
 * Uses the JDK's built-in com.sun.net.httpserver.HttpServer rather than a
 * framework dependency — this project has stayed at two runtime
 * dependencies (Jackson, JUnit) throughout, and a local experimentation
 * server doesn't need more than that buys.
 *
 * Run from the project root:
 *   mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.web.WebServer"
 *
 * LLM_API_KEY is required for anything that calls the reasoner —
 * /api/assess, /api/evaluate, /api/drift-experiment. The server itself
 * starts fine without it; per-request calls just fail with a 502 and a
 * clear error message until it's set. Optional: PORT (default 8080).
 *
 * Endpoints:
 *   GET  /                      the experimentation UI
 *   GET  /api/euc                the loaded EUC definition
 *   GET  /api/dataset             the eval dataset (with ground truth)
 *   POST /api/assess               {organization, grant} -> AssessmentResult
 *   POST /api/evaluate               run the full eval dataset -> per-case scores
 *   POST /api/drift-experiment         run the Week 5 drift experiment -> DriftExperimentReport
 */
public class WebServer {

    private static final String DATASET_PATH = "eval/grant-fit-assessment/dataset/test-cases.json";

    public static void main(String[] args) throws IOException {
        EucDefinition euc = EucLoader.loadGrantFitAssessment();
        List<TestCase> dataset = new TestCaseDataset().loadFromFile(new File(DATASET_PATH));

        String modelName = System.getenv().getOrDefault("LLM_MODEL", "claude-sonnet-4-6");
        GrantFitApplication app = new GrantFitApplication(euc, new LlmFitReasoner(modelName));
        GrantFitEvaluator evaluator = new GrantFitEvaluator(euc);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        server.createContext("/api/euc", new EucHandler(euc));
        server.createContext("/api/dataset", new DatasetHandler(dataset));
        server.createContext("/api/assess", new AssessHandler(app));
        server.createContext("/api/evaluate", new EvaluateHandler(app, evaluator, dataset));
        server.createContext("/api/drift-experiment", new DriftExperimentHandler(euc, dataset));
        server.createContext("/", new StaticFileHandler());

        server.start();

        boolean keySet = System.getenv("LLM_API_KEY") != null && !System.getenv("LLM_API_KEY").isBlank();
        System.out.println("EUC web server started on http://localhost:" + port);
        System.out.println("Model: " + modelName);
        System.out.println("LLM_API_KEY: " + (keySet
                ? "set"
                : "NOT SET -- /api/assess, /api/evaluate, and /api/drift-experiment will 502 until it is"));
    }
}
