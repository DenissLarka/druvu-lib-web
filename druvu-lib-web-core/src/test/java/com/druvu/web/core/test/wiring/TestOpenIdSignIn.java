package com.druvu.web.core.test.wiring;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.OpenIdAuthentication;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.api.config.WebConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The OpenID Connect round trip against a fake provider: the redirect out, the redirect back with a code, the code
 * exchange Jetty does server-side, the session that results, the claims a handler sees, and logout.
 */
public class TestOpenIdSignIn {
    private static final Gson GSON = new Gson();

    private FakeOpenIdProvider provider;
    private BootedApp app;

    @BeforeClass
    public void boot() {
        provider = new FakeOpenIdProvider();
        WebConfig config = WebConfig.builder()
                .port(0)
                .urlConfig(UrlConfig.from(PlainHandler.class))
                .urlConfig(UrlConfig.signedIn(ProfileHandler.class))
                .urlConfig(UrlConfig.signedIn(WhoamiHandler.class))
                .urlConfig(UrlConfig.from(SecretHandler.class, "secret:read"))
                .authConfig(AuthConfig.builder()
                        .people(OpenIdAuthentication.of(
                                provider.issuer(), FakeOpenIdProvider.CLIENT_ID, FakeOpenIdProvider.CLIENT_SECRET))
                        .permissions(subject ->
                                FakeOpenIdProvider.SUBJECT.equals(subject) ? Set.of("secret:read") : Set.of())
                        .build())
                .build();
        app = BootedApp.start(config, "/t");
    }

    @AfterClass
    public void stop() throws Exception {
        app.close();
        provider.close();
    }

    @Test
    public void aPublicRouteNeedsNoSignIn() throws IOException, InterruptedException {
        assertThat(browser()
                        .send(get(app.uri("/plain")), HttpResponse.BodyHandlers.ofString())
                        .statusCode())
                .isEqualTo(200);
    }

    @Test
    public void theRoundTripSignsInKnowsTheClaimsAndLogsOut() throws IOException, InterruptedException {
        HttpClient browser = browser();

        HttpResponse<String> toProvider = browser.send(get(app.uri("/profile")), HttpResponse.BodyHandlers.ofString());
        assertThat(toProvider.statusCode())
                .as("anonymous on a signed-in route is sent to the provider")
                .isIn(302, 303);
        String authorize = location(toProvider);
        assertThat(authorize).startsWith(provider.issuer() + "/authorize?");
        assertThat(authorize).contains("client_id=" + FakeOpenIdProvider.CLIENT_ID);
        assertThat(authorize).contains("redirect_uri=").contains("%2Ft%2Fauth%2Fcallback");

        HttpResponse<String> backWithCode =
                browser.send(get(URI.create(authorize)), HttpResponse.BodyHandlers.ofString());
        assertThat(backWithCode.statusCode()).isIn(302, 303);
        String callback = location(backWithCode);
        assertThat(callback)
                .startsWith(app.uri("/auth/callback").toString())
                .contains("code=" + FakeOpenIdProvider.CODE);

        HttpResponse<String> signedIn = browser.send(get(URI.create(callback)), HttpResponse.BodyHandlers.ofString());
        assertThat(signedIn.statusCode())
                .as("the code is exchanged server-side, then back to where we were")
                .isIn(302, 303);
        assertThat(location(signedIn)).endsWith("/t/profile");

        HttpResponse<String> profile = browser.send(get(app.uri("/profile")), HttpResponse.BodyHandlers.ofString());
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(profile.body().strip()).isEqualTo("profile of " + FakeOpenIdProvider.SUBJECT);

        HttpResponse<String> who = browser.send(get(app.uri("/whoami")), HttpResponse.BodyHandlers.ofString());
        Map<String, String> seen = GSON.fromJson(who.body(), new TypeToken<Map<String, String>>() {}.getType());
        assertThat(seen)
                .containsEntry("subject", FakeOpenIdProvider.SUBJECT)
                .containsEntry("email", FakeOpenIdProvider.EMAIL)
                .containsEntry("displayName", FakeOpenIdProvider.NAME)
                .containsEntry("permissions", "secret:read");

        assertThat(browser.send(get(app.uri("/secret")), HttpResponse.BodyHandlers.ofString())
                        .statusCode())
                .as("the permission store's answer travels with the session")
                .isEqualTo(200);

        HttpResponse<String> logout = browser.send(get(app.uri("/auth/logout")), HttpResponse.BodyHandlers.ofString());
        assertThat(logout.statusCode()).isIn(302, 303);
        assertThat(location(logout)).endsWith("/t/");

        HttpResponse<String> again = browser.send(get(app.uri("/profile")), HttpResponse.BodyHandlers.ofString());
        assertThat(again.statusCode()).as("the session is gone").isIn(302, 303);
        assertThat(location(again)).startsWith(provider.issuer() + "/authorize?");
    }

    /** A browser-like client: keeps cookies, follows no redirect by itself so every hop can be asserted. */
    private static HttpClient browser() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(new CookieManager())
                .build();
    }

    private static HttpRequest get(URI uri) {
        return HttpRequest.newBuilder(uri).GET().build();
    }

    private static String location(HttpResponse<String> response) {
        return response.headers().firstValue("location").orElseThrow();
    }
}
