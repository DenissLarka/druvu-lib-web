package com.druvu.web.core.security;

import com.druvu.web.api.auth.Authentication;
import com.druvu.web.api.auth.BasicAuthentication;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;

/**
 * The Jetty authenticator for a way of signing in.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class Authenticators {

    private Authenticators() {}

    static Authenticator forPeople(Authentication people) {
        return switch (people) {
            case BasicAuthentication basic -> new BasicAuthenticator();
        };
    }

    static String realmOf(Authentication people) {
        return switch (people) {
            case BasicAuthentication basic -> basic.realm();
        };
    }
}
