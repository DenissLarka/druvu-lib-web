package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/** Answers {@code /stray-redirect} by redirecting to {@link UnroutedHandler}, which is no route: refused. */
public final class StrayRedirectHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.sendRedirect(UnroutedHandler.class);
    }
}
