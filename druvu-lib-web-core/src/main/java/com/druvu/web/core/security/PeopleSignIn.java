package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.BasicAuthentication;
import com.druvu.web.api.auth.OpenIdAuthentication;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;

/**
 * The Jetty pieces behind the way people sign in: the authenticator that talks to the browser, the login service that
 * says who someone is and what they may do, and the realm name challenges carry.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
record PeopleSignIn(LoginAuthenticator authenticator, LoginService loginService, String realm) {

    static PeopleSignIn of(AuthConfig auth) {
        return switch (auth.people()) {
            case BasicAuthentication basic ->
                new PeopleSignIn(new BasicAuthenticator(), LoginServices.forBasic(basic, auth), basic.realm());
            case OpenIdAuthentication openId -> OpenIdSetup.signIn(openId, auth);
        };
    }
}
