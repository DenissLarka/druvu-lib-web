package com.druvu.web.example.mcp;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/**
 * The one page for people, plain text: what this server is and where the endpoint is.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public final class Home implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.commit("text/plain", """
                druvu-lib-web MCP example
                Endpoint: POST /mcp with the bearer token printed at boot.
                JSON-RPC 2.0 over Streamable HTTP, MCP protocol 2025-06-18. Tools: echo, time_now.
                """);
    }
}
