package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/** Answers {@code /hello}: the {@code ?who=} parameter becomes {@code $name} in {@code hello.php}. */
public final class HelloHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        request.setAttribute("name", request.paramInfo().getOrDefault("who", "stranger"));
    }
}
