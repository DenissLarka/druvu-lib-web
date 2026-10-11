package com.druvu.web.core.test.wiring;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.BearerAuthentication;
import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.WebConfig;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * An application with no browser side: machines sign in with a token, the rest is public, nobody is asked for a
 * password.
 */
public class TestMachinesOnly {
    private BootedApp app;
    private HttpClient http;

    @BeforeClass
    public void boot() {
        WebConfig config = WebConfig.builder()
                .port(0)
                .route(Route.forEveryone(PlainHandler.class))
                .route(Route.forMachines(ApiStatusHandler.class))
                .authConfig(AuthConfig.builder()
                        .machines(new BearerAuthentication(
                                token -> "tok-robot".equals(token) ? Optional.of("robot") : Optional.empty()))
                        .build())
                .build();
        app = BootedApp.start(config, "/m");
        http = HttpClient.newHttpClient();
    }

    @AfterClass
    public void stop() {
        app.close();
    }

    @Test
    public void thePublicRouteNeedsNothing() throws IOException, InterruptedException {
        assertThat(send(HttpRequest.newBuilder(app.uri("/plain")).GET()).statusCode())
                .isEqualTo(200);
    }

    @Test
    public void theMachineRouteChallengesWithTheDefaultRealm() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder(app.uri("/api-status")).GET());
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate"))
                .contains("Bearer realm=\"" + AuthConfig.DEFAULT_REALM + "\"");
    }

    @Test
    public void aKnownTokenIsServed() throws IOException, InterruptedException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(app.uri("/api-status"))
                .header("Authorization", "Bearer tok-robot")
                .GET());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"ok\"");
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
