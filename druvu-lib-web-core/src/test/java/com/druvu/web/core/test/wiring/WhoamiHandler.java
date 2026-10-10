package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.google.gson.Gson;
import java.util.Map;

/** Answers {@code /whoami} for a signed-in user with what the handler can see of the identity, as JSON. */
public final class WhoamiHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        var user = request.user().orElseThrow();
        Map<String, String> seen = Map.of(
                "subject", user.subject(),
                "email", user.email().orElse(""),
                "displayName", user.displayName().orElse(""),
                "permissions", String.join(",", user.getPermissions()));
        response.commitContent("application/json", new Gson().toJson(seen));
    }
}
