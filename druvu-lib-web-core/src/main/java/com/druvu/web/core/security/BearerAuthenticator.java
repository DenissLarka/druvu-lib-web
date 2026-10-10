package com.druvu.web.core.security;

import com.druvu.web.api.auth.PermissionStore;
import com.druvu.web.api.auth.TokenStore;
import java.util.Optional;
import javax.security.auth.Subject;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.NamePrincipal;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 * Machines sign in with {@code Authorization: Bearer <token>}: the token store says whose it is, the permission store
 * what they may do, and nothing is kept between requests, so no session is ever created for a machine.
 *
 * <p>Extends Jetty's login authenticator only so that a public route defers authentication the same way Basic does; the
 * login service is not used, the identity is built here.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class BearerAuthenticator extends LoginAuthenticator {

    static final String TYPE = "BEARER";
    private static final String SCHEME = "Bearer ";

    private final TokenStore tokens;
    private final PermissionStore permissions;
    private final String realm;

    BearerAuthenticator(TokenStore tokens, PermissionStore permissions, String realm) {
        this.tokens = tokens;
        this.permissions = permissions;
        this.realm = realm;
    }

    @Override
    public String getAuthenticationType() {
        return TYPE;
    }

    @Override
    public AuthenticationState validateRequest(Request request, Response response, Callback callback) {
        Optional<UserIdentity> machine = presentedToken(request).flatMap(this::identityOf);
        if (machine.isPresent()) {
            return new UserAuthenticationSucceeded(TYPE, machine.get());
        }
        if (response.isCommitted()) {
            return AuthenticationState.SEND_FAILURE;
        }
        response.getHeaders().put(HttpHeader.WWW_AUTHENTICATE, "Bearer realm=\"" + realm + "\"");
        Response.writeError(request, response, callback, getUnauthorizedStatusCode());
        return AuthenticationState.CHALLENGE;
    }

    private static Optional<String> presentedToken(Request request) {
        String header = request.getHeaders().get(HttpHeader.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            return Optional.empty();
        }
        String token = header.substring(SCHEME.length()).strip();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private Optional<UserIdentity> identityOf(String token) {
        return tokens.subjectOf(token).map(subject -> {
            NamePrincipal principal = new NamePrincipal(subject);
            Subject javaSubject = new Subject();
            javaSubject.getPrincipals().add(principal);
            String[] roles = permissions.permissions(subject).toArray(String[]::new);
            return _identityService.newUserIdentity(javaSubject, principal, roles);
        });
    }
}
