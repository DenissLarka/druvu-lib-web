package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/** Answers {@code /redirect} by sending the browser to {@code /t/plain}; it has no template of its own. */
public final class RedirectHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.sendRedirect("/t/plain");
    }
}
