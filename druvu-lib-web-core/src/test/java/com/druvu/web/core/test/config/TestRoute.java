package com.druvu.web.core.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.web.api.config.Audience;
import com.druvu.web.api.config.Route;
import com.druvu.web.core.test.wiring.PlainHandler;
import java.util.Set;
import org.testng.annotations.Test;

/** Routes: the URL from the class name, who each factory admits, and the one combination refused. */
public class TestRoute {

    @Test
    public void everyoneNeedsNoSignIn() {
        Route<PlainHandler> route = Route.forEveryone(PlainHandler.class);
        assertThat(route.url()).isEqualTo("plain");
        assertThat(route.audience()).isEqualTo(Audience.EVERYONE);
        assertThat(route.requiresSignIn()).isFalse();
        assertThat(route.permissions()).isEmpty();
    }

    @Test
    public void peopleSignInWithOrWithoutPermissions() {
        assertThat(Route.forSignedIn(PlainHandler.class).requiresSignIn()).isTrue();
        assertThat(Route.forSignedIn(PlainHandler.class).permissions()).isEmpty();
        Route<PlainHandler> orders = Route.forSignedIn(PlainHandler.class, "orders:read", "orders:read", "admin:all");
        assertThat(orders.audience()).isEqualTo(Audience.PEOPLE);
        assertThat(orders.permissions()).containsExactlyInAnyOrder("orders:read", "admin:all");
    }

    @Test
    public void machinesAlwaysSignIn() {
        Route<PlainHandler> route = Route.forMachines(PlainHandler.class);
        assertThat(route.audience()).isEqualTo(Audience.MACHINES);
        assertThat(route.requiresSignIn()).isTrue();
    }

    @Test
    public void aPermissionOnARouteForEveryoneIsRefused() {
        assertThatThrownBy(() -> new Route<>("plain", PlainHandler.class, Set.of("orders:read"), Audience.EVERYONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("implies signing in");
    }
}
