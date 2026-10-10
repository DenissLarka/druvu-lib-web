package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.OpenIdAuthentication;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.security.openid.OpenIdConfiguration;
import org.eclipse.jetty.security.openid.OpenIdLoginService;

/**
 * OpenID Connect through Jetty's own module: the provider is found by discovery, the authenticator does the redirect
 * dance and keeps the signed-in user in the session, and permissions come from the application's store through the
 * wrapped login service.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
final class OpenIdSetup {

    private OpenIdSetup() {}

    static PeopleSignIn signIn(OpenIdAuthentication openId, AuthConfig auth) {
        OpenIdConfiguration configuration =
                new OpenIdConfiguration(openId.issuer(), openId.clientId(), openId.clientSecret());
        configuration.addScopes(openId.scopes().toArray(String[]::new));
        configuration.setAuthenticateNewUsers(true);
        OpenIdLoginService loginService =
                new OpenIdLoginService(configuration, new PermissionLoginService(auth.permissions()));
        OpenIdAuthenticator authenticator = new OpenIdAuthenticator(configuration, openId.callbackPath(), null, null);
        return new PeopleSignIn(authenticator, loginService, "OpenID " + openId.issuer());
    }
}
