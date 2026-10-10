package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.AuthUserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.UserIdentity;

/**
 * The signed-in user of a request, if any, read from what Jetty established.
 *
 * <p>The permissions are the roles Jetty attached at login, which is the one place core reaches below the servlet API
 * ({@link ServletApiRequest}); when a request is not Jetty's own, the {@code PermissionStore} is asked instead.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public final class Identities {

    private Identities() {}

    public static Optional<AuthUserIdentity> of(HttpServletRequest request, AuthConfig auth) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return Optional.empty();
        }
        String subject = principal.getName();
        Set<String> permissions =
                rolesOf(request).orElseGet(() -> auth.permissions().permissions(subject));
        return Optional.of(new SignedInUser(subject, permissions, Optional.empty(), Optional.empty()));
    }

    private static Optional<Set<String>> rolesOf(HttpServletRequest request) {
        if (request instanceof ServletApiRequest jetty
                && AuthenticationState.getAuthenticationState(jetty.getRequest())
                        instanceof AuthenticationState.Succeeded succeeded) {
            UserIdentity identity = succeeded.getUserIdentity();
            return Optional.of(Set.of(identity.getRoles()));
        }
        return Optional.empty();
    }
}
