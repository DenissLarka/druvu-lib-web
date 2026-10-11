package com.druvu.web.api.config;

import com.druvu.web.api.utils.HandlerNameConvention;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * A route: a handler, the URL its class name gives it, and who may reach it.
 *
 * <pre>{@code
 * Route.forEveryone(HomeHandler.class)                      // public
 * Route.forSignedIn(AccountHandler.class)                   // any signed-in person
 * Route.forSignedIn(OrdersHandler.class, "orders:read")     // signed in, with this permission
 * Route.forMachines(GenerateHandler.class, "ea:generate")   // a bearer token, with this permission
 * }</pre>
 *
 * <p>{@code HomeHandler} answers {@code /home}; the first route given to a {@link WebConfig} answers the context root
 * as well. Permissions are all required. A permission implies signing in, so {@link #forEveryone} takes none.
 *
 * @param url the first path segment, from the handler's class name
 * @param handlerClass the handler, instantiated per request through its no-arg constructor
 * @param permissions what a caller must hold, all of them; empty for none
 * @param audience who the route is for, which decides how a caller signs in
 * @author Deniss Larka <br>
 *     on 21 Apr 2024
 */
public record Route<T extends UrlHandler>(
        String url, Class<T> handlerClass, Set<String> permissions, Audience audience) {

    public Route {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(handlerClass, "handlerClass");
        Objects.requireNonNull(audience, "audience");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        if (audience == Audience.EVERYONE && !permissions.isEmpty()) {
            throw new IllegalArgumentException(
                    "A permission implies signing in: /" + url + " is for everyone but asks for " + permissions);
        }
    }

    /** Whether a caller has to be signed in at all. */
    public boolean requiresSignIn() {
        return audience != Audience.EVERYONE;
    }

    /** A public route: anyone may reach it, signed in or not. */
    public static <T extends UrlHandler> Route<T> forEveryone(Class<T> handlerClass) {
        return new Route<>(urlOf(handlerClass), handlerClass, Set.of(), Audience.EVERYONE);
    }

    /** A route for signed-in people holding every permission given; none means any signed-in person. */
    public static <T extends UrlHandler> Route<T> forSignedIn(Class<T> handlerClass, String... permissions) {
        return new Route<>(urlOf(handlerClass), handlerClass, toSet(permissions), Audience.PEOPLE);
    }

    /** A route for machines: a bearer token is always required, plus every permission given. */
    public static <T extends UrlHandler> Route<T> forMachines(Class<T> handlerClass, String... permissions) {
        return new Route<>(urlOf(handlerClass), handlerClass, toSet(permissions), Audience.MACHINES);
    }

    private static String urlOf(Class<?> handlerClass) {
        return HandlerNameConvention.translate(handlerClass);
    }

    private static Set<String> toSet(String[] permissions) {
        return permissions == null ? Set.of() : Set.copyOf(Arrays.asList(permissions));
    }
}
