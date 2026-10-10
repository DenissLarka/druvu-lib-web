package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.api.config.WebConfig;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs sign-in on a context: a session handler and a security handler built from the {@code AuthConfig} and the
 * routes. Both run before any servlet, so an anonymous visitor to a protected route is challenged by Jetty, and what
 * reaches the dispatcher is either public or already signed in.
 *
 * <p>Authorisation stays with the dispatcher on purpose: Jetty's role constraints grant on <em>any</em> listed role,
 * while a route's permissions are all required, so Jetty answers "who" and {@code HandlerUtils} answers "may".
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public final class SecuritySetup {

    private static final Logger LOG = LoggerFactory.getLogger(SecuritySetup.class);

    private SecuritySetup() {}

    public static void install(ServletContextHandler context, WebConfig config) {
        AuthConfig auth = config.authConfig();
        if (auth == null) {
            warnAboutProtectedRoutes(config);
            LOG.info("No AuthConfig: every route is public");
            return;
        }
        SecurityHandler.PathMapped security = new SecurityHandler.PathMapped();
        Constraints.forRoutes(config.urlConfigs()).forEach(security::put);
        // The identity service comes from the login service; Jetty insists the two share one.
        security.setLoginService(LoginServices.forPeople(auth));
        security.setAuthenticator(Authenticators.forPeople(auth.people()));
        security.setRealmName(Authenticators.realmOf(auth.people()));
        security.setSessionRenewedOnAuthentication(true);
        context.setSessionHandler(Sessions.handler(auth));
        context.setSecurityHandler(security);
    }

    private static void warnAboutProtectedRoutes(WebConfig config) {
        for (UrlConfig<?> route : config.urlConfigs()) {
            if (route.requiresSignIn()) {
                LOG.warn("Route /{} requires sign-in but there is no AuthConfig: nobody can reach it", route.url());
            }
        }
    }
}
