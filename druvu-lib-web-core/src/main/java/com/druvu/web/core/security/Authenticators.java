package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.Authentication;
import com.druvu.web.api.auth.BasicAuthentication;
import com.druvu.web.api.config.Audience;
import com.druvu.web.api.config.UrlConfig;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;

/**
 * The Jetty authenticator for a configuration: the people one alone, or routed between it and the bearer one when the
 * application has routes for machines.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class Authenticators {

    private Authenticators() {}

    static Authenticator forConfig(AuthConfig auth, List<UrlConfig> routes) {
        LoginAuthenticator people = forPeople(auth.people());
        return auth.machines()
                .map(bearer -> new BearerAuthenticator(bearer.tokens(), auth.permissions(), realmOf(auth.people())))
                .<Authenticator>map(machines -> new RoutedAuthenticator(people, machines, machineRoutes(routes)))
                .orElse(people);
    }

    static LoginAuthenticator forPeople(Authentication people) {
        return switch (people) {
            case BasicAuthentication basic -> new BasicAuthenticator();
        };
    }

    static String realmOf(Authentication people) {
        return switch (people) {
            case BasicAuthentication basic -> basic.realm();
        };
    }

    private static Set<String> machineRoutes(List<UrlConfig> routes) {
        return routes.stream()
                .filter(route -> route.audience() == Audience.MACHINES)
                .map(UrlConfig::url)
                .collect(Collectors.toSet());
    }
}
