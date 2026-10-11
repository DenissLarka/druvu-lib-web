package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.config.Audience;
import com.druvu.web.api.config.Route;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;

/**
 * The Jetty authenticator for a configuration: the people one alone, the bearer one alone for an application that
 * serves machines only, or routed between the two when it has both.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class Authenticators {

    private Authenticators() {}

    static Authenticator forConfig(AuthConfig auth, Optional<PeopleSignIn> people, List<Route<?>> routes) {
        String realm = people.map(PeopleSignIn::realm).orElse(AuthConfig.DEFAULT_REALM);
        Optional<LoginAuthenticator> machines =
                auth.machines().map(bearer -> new BearerAuthenticator(bearer.tokens(), auth.permissions(), realm));
        if (people.isPresent() && machines.isPresent()) {
            return new RoutedAuthenticator(people.get().authenticator(), machines.get(), machineRoutes(routes));
        }
        if (people.isPresent()) {
            return people.get().authenticator();
        }
        return machines.orElseThrow(() -> new IllegalStateException("No way to sign in: neither people nor machines"));
    }

    private static Set<String> machineRoutes(List<Route<?>> routes) {
        return routes.stream()
                .filter(route -> route.audience() == Audience.MACHINES)
                .map(Route::url)
                .collect(Collectors.toSet());
    }
}
