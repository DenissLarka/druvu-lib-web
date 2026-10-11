package com.druvu.web.example.mcp;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

/**
 * The MCP endpoint. {@link McpTransport} reads the request into a call, or into the reply it already knows;
 * {@link McpServer} answers a call; the {@link Reply} writes itself. The route is for machines, so Jetty has checked
 * the bearer token before this runs.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public final class McpHandler implements HttpHandler {

    private static final McpServer SERVER = new McpServer();

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        Reply reply =
                switch (McpTransport.read(request)) {
                    case McpCall call -> SERVER.answer(call);
                    case Reply early -> early;
                };
        reply.writeTo(response);
    }
}
