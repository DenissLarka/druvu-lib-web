package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.OpenIdAuthentication;
import com.druvu.web.api.config.WebConfig;
import java.util.Optional;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
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
        if (config.authConfig().isEmpty()) {
            LOG.info("No AuthConfig: every route is public");
            return;
        }
        AuthConfig auth = config.authConfig().get();
        Optional<PeopleSignIn> people = auth.people().map(how -> PeopleSignIn.of(how, auth));
        SecurityHandler.PathMapped security = new SecurityHandler.PathMapped();
        Constraints.forRoutes(config.routes()).forEach(security::put);
        // The identity service comes from the login service; Jetty insists the two share one. With machines only,
        // the bearer authenticator builds identities itself and the login service only answers permissions.
        security.setLoginService(
                people.map(PeopleSignIn::loginService).orElseGet(() -> new PermissionLoginService(auth.permissions())));
        security.setAuthenticator(Authenticators.forConfig(auth, people, config.routes()));
        security.setRealmName(people.map(PeopleSignIn::realm).orElse(AuthConfig.DEFAULT_REALM));
        security.setSessionRenewedOnAuthentication(true);
        context.setSessionHandler(Sessions.handler(auth));
        context.setSecurityHandler(security);
        if (auth.people().orElse(null) instanceof OpenIdAuthentication openId) {
            context.addServlet(
                    new ServletHolder("logout", new LogoutServlet(openId.afterLogout())), openId.logoutPath());
        }
    }
}
