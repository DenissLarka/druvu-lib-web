package com.druvu.web.example.handlers;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.google.gson.Gson;
import java.util.Map;

/**
 * A route for machines: answers whoever holds a valid token with the subject the token belongs to.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public class ApiPingHandler implements HttpHandler {
    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        String subject = request.user().map(user -> user.subject()).orElse("nobody");
        response.commitContent("application/json", GSON.toJson(Map.of("pong", subject)));
    }
}
