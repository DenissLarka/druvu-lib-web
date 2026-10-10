package com.druvu.web.api.config;

import com.druvu.web.api.utils.HandlerNameConvention;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A route: the handler, the URL its name gives it, and who may reach it. No permissions and not {@code signedIn} means
 * public; a permission implies signing in.
 *
 * @author : Deniss Larka on 21 April 2024
 */
public record UrlConfig<T extends UrlHandler>(
        String url, Class<T> urlHandlerClass, boolean _default, Set<String> permissions, boolean signedIn) {

    public UrlConfig {
        Objects.requireNonNull(url);
        Objects.requireNonNull(urlHandlerClass);
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
                HandlerNameConvention.translate(urlHandlerClass), urlHandlerClass, false, toSet(permissions), false);
    }

    public static <T extends UrlHandler> UrlConfig<T> from(
            Class<T> urlHandlerClass, boolean _default, String... permissions) {
        return new UrlConfig<>(
                HandlerNameConvention.translate(urlHandlerClass), urlHandlerClass, _default, toSet(permissions), false);
    }

    /** A route for any signed-in user, with no particular permission: a profile page, an account page. */
    public static <T extends UrlHandler> UrlConfig<T> signedIn(Class<T> urlHandlerClass) {
        return new UrlConfig<>(
                HandlerNameConvention.translate(urlHandlerClass), urlHandlerClass, false, Set.of(), true);
    }

    private static Set<String> toSet(String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(permissions));
    }
}
