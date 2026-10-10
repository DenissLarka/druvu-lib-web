package com.druvu.web.core.test.wiring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.BearerAuthentication;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.api.config.WebConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The framework's wiring exercised over real HTTP against a booted server: routing by handler name, the
 * handler-to-template channel, committed responses, the not-found and error paths, Basic auth with per-URL permissions,
 * static files, and the WebSocket upgrade with its echo through a handler.
 */
public class TestWiring {
    private static final String PERMISSION = "secret:read";
    /**
     * Tokens as a machine would present them; alice holds the permission, carol is nobody the permission store knows.
     */
    private static final Map<String, String> TOKENS = Map.of("tok-alice", "alice", "tok-carol", "carol");

    private static final Gson GSON = new Gson();

    private BootedApp app;
    private HttpClient http;

    @BeforeClass
    public void boot() {
        WebConfig config = WebConfig.builder()
                .port(0)
                .urlConfig(UrlConfig.from(PlainHandler.class))
                .urlConfig(UrlConfig.from(HelloHandler.class))
                .urlConfig(UrlConfig.from(JsonHandler.class))
                .urlConfig(UrlConfig.from(SecretHandler.class, PERMISSION))
                .urlConfig(UrlConfig.signedIn(ProfileHandler.class))
                .urlConfig(UrlConfig.from(FormHandler.class))
                .urlConfig(UrlConfig.from(BrokenHandler.class))
                .urlConfig(UrlConfig.from(EchoSocketHandler.class))
                .urlConfig(UrlConfig.from(SecretSocketHandler.class, PERMISSION))
                .urlConfig(UrlConfig.forMachines(ApiStatusHandler.class))
                .urlConfig(UrlConfig.forMachines(ApiSecretHandler.class, PERMISSION))
                .authConfig(AuthConfig.builder()
                        .basicAuth()
                        .user("alice", "pw", PERMISSION)
                        .user("bob", "pw", "other:permission")
                        .machines(new BearerAuthentication(token -> Optional.ofNullable(TOKENS.get(token))))
                        .build())
                .build();
        app = BootedApp.start(config, "/t");
        http = HttpClient.newHttpClient();
    }

    @AfterClass
    public void stop() throws Exception {
        app.close();
    }

    @Test
    public void aHandlerIsRoutedByItsNameAndRendersItsTemplate() {
        HttpResponse<String> response = get("/plain");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).startsWith("text/html");
        assertThat(response.body()).contains("<p>plain page</p>");
    }

    @Test
    public void theContextRootRendersTheFirstRegisteredHandler() {
        HttpResponse<String> response = get("/");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<p>plain page</p>");
    }

    @Test
    public void aRequestAttributeSetByTheHandlerReachesTheTemplate() {
        HttpResponse<String> response = get("/hello?who=Ada");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().strip()).isEqualTo("Hello, Ada!");
    }

    @Test
    public void aCommittedResponseSkipsTheTemplate() {
        HttpResponse<String> response = get("/json");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).startsWith("application/json");
        assertThat(response.body()).isEqualTo("{\"ok\":true}");
    }

    @Test
    public void anUnknownPathIsNotFound() {
        assertThat(get("/nope").statusCode()).isEqualTo(404);
    }

    @Test
    public void aProtectedHandlerChallengesWithoutCredentials() {
        HttpResponse<String> response = get("/secret");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("www-authenticate"))
                .hasValueSatisfying(challenge -> assertThat(challenge).startsWithIgnoringCase("Basic"));
    }

    @Test
    public void aProtectedHandlerAcceptsAUserHoldingThePermission() {
        HttpResponse<String> response = getAs("alice", "/secret");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("top secret");
    }

    @Test
    public void aProtectedHandlerRefusesAUserWithoutThePermission() {
        assertThat(getAs("bob", "/secret").statusCode()).isEqualTo(403);
    }

    @Test
    public void aSignedInRouteTakesAnyUserAndKnowsWho() {
        assertThat(get("/profile").statusCode()).isEqualTo(401);
        HttpResponse<String> response = getAs("bob", "/profile");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().strip()).isEqualTo("profile of bob");
    }

    @Test
    public void postedFieldsReachTheTemplateThroughPost() {
        HttpResponse<String> response = send(HttpRequest.newBuilder(app.uri("/form"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("q=42")));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().strip()).isEqualTo("q=42");
    }

    @Test
    public void aStaticFileIsServedFromTheStaticPath() {
        HttpResponse<String> response = get("/static/test.css");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).startsWith("text/css");
        assertThat(response.body()).contains("test asset");
    }

    @Test
    public void aTemplateErrorIsContainedAndNamesNoFile() {
        HttpResponse<String> response = get("/broken");
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).doesNotContain("broken.php").doesNotContain("Exception");
    }

    @Test
    public void aMachineRouteChallengesForABearerToken() {
        HttpResponse<String> response = get("/api-status");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("www-authenticate"))
                .hasValueSatisfying(challenge -> assertThat(challenge).startsWith("Bearer"));
        assertThat(getAs("alice", "/api-status").statusCode())
                .as("Basic credentials mean nothing on a machine route")
                .isEqualTo(401);
    }

    @Test
    public void aMachineWithAKnownTokenIsServed() {
        HttpResponse<String> response = withToken("tok-alice", "/api-status");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"ok\"}");
        assertThat(withToken("tok-nobody", "/api-status").statusCode()).isEqualTo(401);
    }

    @Test
    public void aMachineRouteChecksPermissionsLikeAnyOther() {
        assertThat(withToken("tok-alice", "/api-secret").statusCode()).isEqualTo(200);
        assertThat(withToken("tok-carol", "/api-secret").statusCode()).isEqualTo(403);
    }

    @Test
    public void aPeopleRouteIgnoresABearerToken() {
        assertThat(withToken("tok-alice", "/secret").statusCode()).isEqualTo(401);
    }

    @Test
    public void aWebSocketHandlerEchoesThroughItsSession()
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, String> reply = json(roundTrip("/echo-socket", "{\"text\":\"ping\"}", null));
        assertThat(reply).containsEntry("echo", "ping").containsEntry("user", "anonymous");
    }

    @Test
    public void aProtectedWebSocketRefusesTheUpgradeWithoutCredentials() {
        assertThatThrownBy(() -> roundTrip("/secret-socket", "{\"text\":\"ping\"}", null))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(WebSocketHandshakeException.class);
    }

    @Test
    public void aProtectedWebSocketKnowsItsAuthenticatedUser()
            throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, String> reply = json(roundTrip("/secret-socket", "{\"text\":\"ping\"}", "alice"));
        assertThat(reply).containsEntry("echo", "ping").containsEntry("user", "alice");
    }

    private HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(app.uri(path)).GET());
    }

    private HttpResponse<String> getAs(String user, String path) {
        return send(HttpRequest.newBuilder(app.uri(path))
                .header("Authorization", basic(user))
                .GET());
    }

    private HttpResponse<String> withToken(String token, String path) {
        return send(HttpRequest.newBuilder(app.uri(path))
                .header("Authorization", "Bearer " + token)
                .GET());
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static Map<String, String> json(String text) {
        return GSON.fromJson(text, new TypeToken<Map<String, String>>() {}.getType());
    }

    private static String contentType(HttpResponse<String> response) {
        return response.headers().firstValue("content-type").orElse("");
    }

    /** Every test user has the password {@code pw}. */
    private static String basic(String user) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":pw").getBytes(StandardCharsets.UTF_8));
    }

    /** Opens a socket, sends one text frame, returns the first complete text reply; {@code user} null = no auth. */
    private String roundTrip(String path, String message, String user)
            throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<String> reply = new CompletableFuture<>();
        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                buffer.append(data);
                if (last) {
                    reply.complete(buffer.toString());
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                reply.completeExceptionally(error);
            }
        };
        WebSocket.Builder builder = http.newWebSocketBuilder();
        if (user != null) {
            builder.header("Authorization", basic(user));
        }
        WebSocket socket = builder.buildAsync(app.wsUri(path), listener).get(10, TimeUnit.SECONDS);
        try {
            socket.sendText(message, true).get(10, TimeUnit.SECONDS);
            return reply.get(10, TimeUnit.SECONDS);
        } finally {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }
}
