package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/** Answers {@code /json} by committing a JSON body, so no template is rendered. */
public final class JsonHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.commitContent("application/json", "{\"ok\":true}");
    }
}
