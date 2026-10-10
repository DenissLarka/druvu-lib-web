package com.druvu.web.api.config;

import com.druvu.web.api.utils.HandlerNameConvention;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A route: the handler, the URL its name gives it, and who may reach it. No permissions and not {@code signedIn} means
 * public; a permission implies signing in; a route for machines always needs a token.
 *
 * @author : Deniss Larka on 21 April 2024
 */
public record UrlConfig<T extends UrlHandler>(
        String url,
        Class<T> urlHandlerClass,
        boolean _default,
        Set<String> permissions,
        boolean signedIn,
        Audience audience) {

    public UrlConfig {
        Objects.requireNonNull(url);
        Objects.requireNonNull(urlHandlerClass);
        Objects.requireNonNull(audience);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    @Override
    public Set<String> permissions() {
        return Collections.unmodifiableSet(permissions);
    }

    /** Whether a visitor has to be signed in at all: asked for by name, or implied by a permission. */
    public boolean requiresSignIn() {
        return signedIn || !permissions.isEmpty();
    }

    /**
     * Create a UrlConfig with permissions. Empty permissions means publicly accessible.
     *
     * @param urlHandlerClass the handler class
     * @param permissions required permissions, all of them; omit for public access
     */
    public static <T extends UrlHandler> UrlConfig<T> from(Class<T> urlHandlerClass, String... permissions) {
        return new UrlConfig<>(
                urlOf(urlHandlerClass), urlHandlerClass, false, toSet(permissions), false, Audience.PEOPLE);
    }

    public static <T extends UrlHandler> UrlConfig<T> from(
            Class<T> urlHandlerClass, boolean _default, String... permissions) {
        return new UrlConfig<>(
                urlOf(urlHandlerClass), urlHandlerClass, _default, toSet(permissions), false, Audience.PEOPLE);
    }

    /** A route for any signed-in user, with no particular permission: a profile page, an account page. */
    public static <T extends UrlHandler> UrlConfig<T> signedIn(Class<T> urlHandlerClass) {
        return new UrlConfig<>(urlOf(urlHandlerClass), urlHandlerClass, false, Set.of(), true, Audience.PEOPLE);
    }

    /** A route for machines: a bearer token is always required, plus the permissions given. */
    public static <T extends UrlHandler> UrlConfig<T> forMachines(Class<T> urlHandlerClass, String... permissions) {
        return new UrlConfig<>(
                urlOf(urlHandlerClass), urlHandlerClass, false, toSet(permissions), true, Audience.MACHINES);
    }

    private static String urlOf(Class<?> urlHandlerClass) {
        return HandlerNameConvention.translate(urlHandlerClass);
    }

    private static Set<String> toSet(String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(permissions));
    }
}
