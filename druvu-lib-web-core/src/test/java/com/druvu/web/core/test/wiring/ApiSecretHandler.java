package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/** A route for machines at {@code /api-secret} that also needs a permission. */
public final class ApiSecretHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.commitContent("application/json", "{\"secret\":true}");
    }
}
