package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.config.Audience;
import com.druvu.web.api.config.UrlConfig;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jetty.security.Authenticator;

/**
 * The Jetty authenticator for a configuration: the people one alone, or routed between it and the bearer one when the
 * application has routes for machines.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class Authenticators {

    private Authenticators() {}

    static Authenticator forConfig(AuthConfig auth, PeopleSignIn people, List<UrlConfig> routes) {
        return auth.machines()
                .map(bearer -> new BearerAuthenticator(bearer.tokens(), auth.permissions(), people.realm()))
                .<Authenticator>map(
                        machines -> new RoutedAuthenticator(people.authenticator(), machines, machineRoutes(routes)))
                .orElse(people.authenticator());
    }

    private static Set<String> machineRoutes(List<UrlConfig> routes) {
        return routes.stream()
                .filter(route -> route.audience() == Audience.MACHINES)
                .map(UrlConfig::url)
                .collect(Collectors.toSet());
    }
}
