package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthUserIdentity;
import java.security.Principal;
import java.util.Optional;
import java.util.Set;

/**
 * The identity a handler sees, as a value.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
record SignedInUser(String subject, Set<String> permissions, Optional<String> email, Optional<String> displayName)
        implements AuthUserIdentity {

    SignedInUser {
        permissions = Set.copyOf(permissions);
    }

    @Override
    public Principal getUserPrincipal() {
        return new Name(subject);
    }

    @Override
    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return subject;
    }

    private record Name(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
