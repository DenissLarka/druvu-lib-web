package com.druvu.web.api.auth;

import java.util.Map;
import java.util.Objects;

/**
 * HTTP Basic authentication with inline users: what a demo and a test want, never what a public site wants.
 *
 * @param realm the name the browser's password prompt shows
 * @param users by user name
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public record BasicAuthentication(String realm, Map<String, BasicUser> users) implements Authentication {

    public BasicAuthentication {
        Objects.requireNonNull(realm, "realm");
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("Basic authentication needs at least one user");
        }
        users = Map.copyOf(users);
    }
}
