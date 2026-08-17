package com.euc.grantfitassessment.web;

import com.euc.core.EucDefinition;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/** GET /api/euc — returns the loaded EUC definition, so the UI can show what's actually driving execution and evaluation. */
class EucHandler implements HttpHandler {

    private final EucDefinition euc;

    EucHandler(EucDefinition euc) {
        this.euc = euc;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonHttpUtil.sendMethodNotAllowed(exchange, "GET");
            return;
        }
        JsonHttpUtil.sendJson(exchange, 200, euc);
    }
}
