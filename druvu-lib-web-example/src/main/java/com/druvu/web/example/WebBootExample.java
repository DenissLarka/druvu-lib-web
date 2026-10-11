package com.druvu.web.example;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.BearerAuthentication;
import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.WebBoot;
import com.druvu.web.example.handlers.AccountHandler;
import com.druvu.web.example.handlers.ApiPingHandler;
import com.druvu.web.example.handlers.Example;
import com.druvu.web.example.handlers.ExampleJsonHandler;
import com.druvu.web.example.handlers.ExampleSocketHandler;
import com.druvu.web.example.handlers.ExampleTable;
import com.druvu.web.example.handlers.PhpBasics;
import com.druvu.web.example.handlers.PhpData;
import com.druvu.web.example.handlers.PhpLoops;
import com.druvu.web.example.handlers.TokensHandler;
import com.druvu.web.example.handlers.WsHandler;

/**
 * The example application: public pages, pages for signed-in people, a route for machines, and Basic sign-in with two
 * inline users. Start it and open {@code http://localhost:8081/web-test/}.
 *
 * @author Deniss Larka <br>
 *     on 21 Apr 2024
 */
public final class WebBootExample {

    public static final int PORT = 8081;

    private WebBootExample() {}

    static void main(String[] args) {
        logConfig();

        WebConfig config = WebConfig.builder()
                .port(PORT)
                // Public; the first route also answers the context root
                .route(Route.forEveryone(Example.class))
                .route(Route.forEveryone(PhpBasics.class))
                .route(Route.forEveryone(PhpLoops.class))
                .route(Route.forEveryone(PhpData.class))
                .route(Route.forEveryone(ExampleTable.class))
                .route(Route.forEveryone(ExampleSocketHandler.class))
                .route(Route.forEveryone(ExampleJsonHandler.class))
                .route(Route.forEveryone(WsHandler.class))
                // Signed-in people
                .route(Route.forSignedIn(AccountHandler.class))
                .route(Route.forSignedIn(TokensHandler.class))
                // Machines, with a bearer token
                .route(Route.forMachines(ApiPingHandler.class))
                .authConfig(AuthConfig.builder()
                        .basicAuth("Example")
                        .user("user", "pass", "generic:permission")
                        .user("admin", "admin", "generic:permission", "admin:permission")
                        .machines(new BearerAuthentication(ExampleTokens.STORE))
                        .build())
                .build();

        new WebBoot(config).start("/web-test");
    }

    private static void logConfig() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
    }
}
