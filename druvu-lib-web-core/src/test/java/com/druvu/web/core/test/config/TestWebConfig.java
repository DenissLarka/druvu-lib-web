package com.druvu.web.core.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.BearerAuthentication;
import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.test.wiring.HelloHandler;
import com.druvu.web.core.test.wiring.PlainHandler;
import java.util.Optional;
import org.testng.annotations.Test;

/** The server configuration builder: its defaults, what it adds to them, and what it refuses. */
public class TestWebConfig {

    @Test
    public void theDefaultsAreLoopbackPort8080AndTheTwoStaticPaths() {
        WebConfig config =
                WebConfig.builder().route(Route.forEveryone(PlainHandler.class)).build();
        assertThat(config.host()).isEmpty();
        assertThat(config.port()).isEqualTo(8080);
        assertThat(config.staticPaths()).containsExactlyInAnyOrder("/static/*", "/webjars/*");
        assertThat(config.globalObjects()).isEmpty();
        assertThat(config.serveFromDirectory()).isEmpty();
        assertThat(config.authConfig()).isEmpty();
    }

    @Test
    public void aStaticPathJoinsTheDefaultsAndAGlobalObjectIsKept() {
        Object service = new Object();
        WebConfig config = WebConfig.builder()
                .route(Route.forEveryone(PlainHandler.class))
                .staticPath("/assets/*")
                .globalObject("service", service)
                .build();
        assertThat(config.staticPaths()).containsExactlyInAnyOrder("/static/*", "/webjars/*", "/assets/*");
        assertThat(config.globalObjects()).containsEntry("service", service);
    }

    @Test
    public void routesKeepTheirOrder() {
        WebConfig config = WebConfig.builder()
                .route(Route.forEveryone(PlainHandler.class))
                .route(Route.forSignedIn(HelloHandler.class))
                .authConfig(AuthConfig.builder().user("u", "p").build())
                .build();
        assertThat(config.routes()).extracting(route -> route.url()).containsExactly("plain", "hello");
    }

    @Test
    public void noRoutesIsRefused() {
        assertThatThrownBy(() -> WebConfig.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No routes");
    }

    @Test
    public void twoRoutesOnOneUrlAreRefusedNamingBoth() {
        assertThatThrownBy(() -> WebConfig.builder()
                        .route(Route.forEveryone(PlainHandler.class))
                        .route(Route.forSignedIn(PlainHandler.class))
                        .authConfig(AuthConfig.builder().user("u", "p").build())
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/plain")
                .hasMessageContaining(PlainHandler.class.getName());
    }

    @Test
    public void aRouteForSignedInPeopleNeedsAWayForPeopleToSignIn() {
        assertThatThrownBy(() -> WebConfig.builder()
                        .route(Route.forSignedIn(PlainHandler.class))
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/plain is for signed-in people");
        assertThatThrownBy(() -> WebConfig.builder()
                        .route(Route.forSignedIn(PlainHandler.class))
                        .authConfig(AuthConfig.builder()
                                .machines(new BearerAuthentication(token -> Optional.empty()))
                                .build())
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no way for people to sign in");
    }

    @Test
    public void aRouteForMachinesNeedsMachines() {
        assertThatThrownBy(() -> WebConfig.builder()
                        .route(Route.forMachines(PlainHandler.class))
                        .authConfig(AuthConfig.builder().user("u", "p").build())
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/plain is for machines");
    }

    @Test
    public void aPortOutsideTheRangeAndAStaticPathWithoutASlashAreRefused() {
        assertThatThrownBy(() -> WebConfig.builder().port(70000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WebConfig.builder().staticPath("assets/*"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
