package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.google.gson.Gson;
import java.util.Map;

/**
 * Answers {@code /body}: GET is refused with 405 and an Allow header, an empty POST is 204, and a POST with a body
 * echoes the method and the body as JSON, with a request header copied to a response header.
 */
public final class BodyHandler implements HttpHandler {
    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        if (!request.isPost()) {
            response.header("Allow", "POST").commit(405);
            return;
        }
        String body = request.body();
        if (body.isEmpty()) {
            response.commit(204);
            return;
        }
        response.header("X-Seen", request.header("X-Probe").orElse("none"));
        response.commit(200, "application/json", GSON.toJson(Map.of("method", request.methodName(), "body", body)));
    }
}
