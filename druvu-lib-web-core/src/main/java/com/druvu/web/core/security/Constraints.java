package com.druvu.web.core.security;

import com.druvu.web.api.config.UrlConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.security.Constraint;

/**
 * Routes to Jetty constraints: a route that requires sign-in is {@code ANY_USER}, the rest is {@code ALLOWED}. Each
 * route covers its path and everything under it, and the context root takes the first route's constraint, because that
 * is the route the root renders.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class Constraints {

    private Constraints() {}

    static Map<String, Constraint> forRoutes(List<UrlConfig> routes) {
        Map<String, Constraint> constraints = new LinkedHashMap<>();
        for (UrlConfig<?> route : routes) {
            Constraint constraint = route.requiresSignIn() ? Constraint.ANY_USER : Constraint.ALLOWED;
            constraints.put("/" + route.url(), constraint);
            constraints.put("/" + route.url() + "/*", constraint);
        }
        if (!routes.isEmpty()) {
            constraints.put("/", constraints.get("/" + routes.getFirst().url()));
        }
        return constraints;
    }
}
