package com.druvu.web.example.handlers;

import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.druvu.web.example.ExampleTokens;
import java.util.List;

/**
 * The signed-in user's own page, reachable by anyone who is signed in: what the application knows about them.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public class AccountHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        AuthUserIdentity user = request.user().orElseThrow();
        request.setAttribute("title", "Account");
        request.setAttribute("subject", user.subject());
        request.setAttribute("email", user.email().orElse(""));
        request.setAttribute("displayName", user.displayName().orElse(""));
        request.setAttribute("permissions", List.copyOf(user.getPermissions()));
        request.setAttribute("tokens", ExampleTokens.STORE.issuedFor(user.subject()));
    }
}
