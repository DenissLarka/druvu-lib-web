package com.druvu.web.core.security;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;

/**
 * One authenticator for the context, choosing by route: a request for a route declared for machines goes to the bearer
 * authenticator, everything else to the people one. Jetty's own {@code MultiAuthenticator} chooses by session, which is
 * a sign-in page's problem, not this one.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class RoutedAuthenticator extends LoginAuthenticator {

    private final LoginAuthenticator people;
    private final LoginAuthenticator machines;
    private final Set<String> machineRoutes;

    /** @param machineRoutes the first path segments, as the dispatcher sees them, of the routes for machines */
    RoutedAuthenticator(LoginAuthenticator people, LoginAuthenticator machines, Set<String> machineRoutes) {
        this.people = people;
        this.machines = machines;
        this.machineRoutes = Set.copyOf(machineRoutes);
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        super.setConfiguration(configuration);
        people.setConfiguration(configuration);
        machines.setConfiguration(configuration);
    }

    @Override
    public String getAuthenticationType() {
        return "ROUTED";
    }

    @Override
    public AuthenticationState validateRequest(Request request, Response response, Callback callback)
            throws ServerAuthException {
        return forPath(Request.getPathInContext(request)).validateRequest(request, response, callback);
    }

    @Override
    public Request prepareRequest(Request request, AuthenticationState authenticationState) {
        return forPath(Request.getPathInContext(request)).prepareRequest(request, authenticationState);
    }

    @Override
    public Constraint.Authorization getConstraintAuthentication(
            String pathInContext, Constraint.Authorization existing, Function<Boolean, Session> getSession) {
        return forPath(pathInContext).getConstraintAuthentication(pathInContext, existing, getSession);
    }

    @Override
    public UserIdentity login(String username, Object password, Request request, Response response) {
        return people.login(username, password, request, response);
    }

    @Override
    public void logout(Request request, Response response) {
        people.logout(request, response);
    }

    private Authenticator forPath(String pathInContext) {
        return machineRoutes.contains(firstSegment(pathInContext)) ? machines : people;
    }

    /** The same rule the dispatcher uses to find a route: the first segment, lower-cased. */
    private static String firstSegment(String pathInContext) {
        String path = pathInContext == null ? "" : pathInContext;
        int start = path.startsWith("/") ? 1 : 0;
        int end = path.indexOf('/', start);
        return path.substring(start, end < 0 ? path.length() : end).toLowerCase(Locale.ENGLISH);
    }
}
