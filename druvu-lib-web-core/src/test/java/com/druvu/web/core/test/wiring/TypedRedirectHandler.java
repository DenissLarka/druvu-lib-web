package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/** Answers {@code /typed-redirect}, at any depth, by redirecting to {@link PlainHandler}'s route. */
public final class TypedRedirectHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.sendRedirect(PlainHandler.class);
    }
}
