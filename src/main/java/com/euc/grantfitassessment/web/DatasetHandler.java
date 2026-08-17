package com.euc.grantfitassessment.web;

import com.euc.grantfitassessment.eval.TestCase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/** GET /api/dataset — returns the eval dataset (with ground truth), so the UI can offer sample cases to prefill the assessment form. */
class DatasetHandler implements HttpHandler {

    private final List<TestCase> dataset;

    DatasetHandler(List<TestCase> dataset) {
        this.dataset = dataset;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonHttpUtil.sendMethodNotAllowed(exchange, "GET");
            return;
        }
        JsonHttpUtil.sendJson(exchange, 200, dataset);
    }
}
