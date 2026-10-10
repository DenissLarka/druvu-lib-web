package com.druvu.web.api.auth;

import java.util.List;
import java.util.Objects;

/**
 * Sign-in through one OpenID Connect provider, found by discovery from its issuer URL: Google alone, or a broker that
 * fronts Google, GitHub and Apple and handles registration. The application never sees a password.
 *
 * <p>The provider sends the browser back to {@code callbackPath} under the application's context; {@code logoutPath}
 * ends the session and sends the browser to {@code afterLogout}, both context-relative.
 *
 * @param issuer the provider's issuer URL, {@code https://accounts.google.com} for instance
 * @param clientId what the provider calls this application
 * @param clientSecret the secret the provider gave with it
 * @param scopes what to ask for beyond {@code openid}; {@code email} and {@code profile} fill the identity's claims
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public record OpenIdAuthentication(
        String issuer,
        String clientId,
        String clientSecret,
        List<String> scopes,
        String callbackPath,
        String logoutPath,
        String afterLogout)
        implements Authentication {

    public static final List<String> DEFAULT_SCOPES = List.of("email", "profile");
    public static final String DEFAULT_CALLBACK_PATH = "/auth/callback";
    public static final String DEFAULT_LOGOUT_PATH = "/auth/logout";
    public static final String DEFAULT_AFTER_LOGOUT = "/";

    public OpenIdAuthentication {
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(clientSecret, "clientSecret");
        scopes = List.copyOf(scopes);
        requirePath(callbackPath, "callbackPath");
        requirePath(logoutPath, "logoutPath");
        requirePath(afterLogout, "afterLogout");
    }

    /** The provider with the default scopes and paths. */
    public static OpenIdAuthentication of(String issuer, String clientId, String clientSecret) {
        return new OpenIdAuthentication(
                issuer,
                clientId,
                clientSecret,
                DEFAULT_SCOPES,
                DEFAULT_CALLBACK_PATH,
                DEFAULT_LOGOUT_PATH,
                DEFAULT_AFTER_LOGOUT);
    }

    private static void requirePath(String path, String name) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException(name + " must start with '/', got " + path);
        }
    }
}
