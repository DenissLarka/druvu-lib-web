package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/** A route for machines at {@code /api-status}: any valid token, a JSON answer. */
public final class ApiStatusHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.commitContent("application/json", "{\"status\":\"ok\"}");
    }
}
