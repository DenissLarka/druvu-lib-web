package com.druvu.web.api.auth;

import java.util.Objects;
import java.util.Set;

/**
 * One inline user of {@link BasicAuthentication}: a password and what the user may do.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public record BasicUser(String password, Set<String> permissions) {

    public BasicUser {
        Objects.requireNonNull(password, "password");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
