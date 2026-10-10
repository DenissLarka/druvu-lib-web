package com.druvu.web.example.handlers;

import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.druvu.web.example.ExampleTokens;

/**
 * API tokens for the signed-in user: a POST issues one and the page shows it once, which is the only time anyone sees
 * it; the store keeps the hash.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public class TokensHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        AuthUserIdentity user = request.user().orElseThrow();
        request.setAttribute("title", "API tokens");
        request.setAttribute("subject", user.subject());
        if ("POST".equalsIgnoreCase(request.method())) {
            request.setAttribute("token", ExampleTokens.STORE.issue(user.subject()));
        }
        request.setAttribute("issued", ExampleTokens.STORE.issuedFor(user.subject()));
    }
}
