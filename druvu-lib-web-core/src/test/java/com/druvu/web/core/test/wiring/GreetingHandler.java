package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/** Answers {@code /greeting} with the application's global object of that name, as text. */
public final class GreetingHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        String greeting = request.globalAttributes().get("greeting");
        response.commitContent("text/plain", greeting);
    }
}
