package com.druvu.web.example.mcp;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.BearerAuthentication;
import com.druvu.web.api.auth.TokenStore;
import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.WebBoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An MCP server on druvu-lib-web: one route for machines speaking JSON-RPC over Streamable HTTP, a bearer token shown
 * once at boot, JSON built and read through druvu-lib-json, and nothing else: no template engine, no browser side.
 * Start it, then
 *
 * <pre>{@code
 * claude mcp add --transport http druvu-example http://localhost:8082/mcp --header "Authorization: Bearer <token>"
 * }</pre>
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public final class McpExample {

    public static final int PORT = 8082;
    private static final Logger LOG = LoggerFactory.getLogger(McpExample.class);

    private McpExample() {}

    static void main(String[] args) {
        BootToken token = BootToken.generate();
        WebBoot boot = new WebBoot(config(PORT, token));
        boot.start("/");
        LOG.info("MCP endpoint: http://localhost:{}/mcp", boot.port());
        LOG.info("Bearer token, shown once: {}", token.token());
        LOG.info(
                "claude mcp add --transport http druvu-example http://localhost:{}/mcp --header \"Authorization: Bearer {}\"",
                boot.port(),
                token.token());
        LOG.info(
                "Walk it: ./druvu-lib-web-examples/druvu-lib-web-example-mcp/Invoke-McpExample.ps1 -Token {}",
                token.token());
    }

    /** The configuration, with the port and the token store open so a test can boot it on a free port. */
    static WebConfig config(int port, TokenStore tokens) {
        return WebConfig.builder()
                .port(port)
                .route(Route.forEveryone(Home.class)) // plain text: what this is, where the endpoint is
                .route(Route.forMachines(McpHandler.class)) // the endpoint; Jetty checks the bearer token first
                .authConfig(AuthConfig.builder()
                        .machines(new BearerAuthentication(tokens))
                        .build())
                .build();
    }
}
