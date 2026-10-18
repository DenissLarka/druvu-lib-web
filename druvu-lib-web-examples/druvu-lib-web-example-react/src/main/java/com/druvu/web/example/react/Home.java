package com.druvu.web.example.react;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/**
 * The root: sends the browser to the app's page, which the static servlet serves like any other file.
 *
 * @author Deniss Larka <br>
 *     on 18 Oct 2026
 */
public final class Home implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.sendRedirect("/index.html");
    }
}
