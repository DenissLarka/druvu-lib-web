package com.druvu.web.core.security;

import com.druvu.web.api.auth.PermissionStore;
import java.security.Principal;
import java.util.Objects;
import java.util.function.Function;
import javax.security.auth.Subject;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Session;

/**
 * A Jetty login service that holds no credentials at all: somebody else has established who the subject is (the OpenID
 * provider), and this only answers what the subject may do, as roles. Every subject is known, with an empty set when
 * the store has nothing for it.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
final class PermissionLoginService implements LoginService {

    private final PermissionStore permissions;
    private IdentityService identityService;

    PermissionLoginService(PermissionStore permissions) {
        this.permissions = permissions;
    }

    @Override
    public String getName() {
        return "permissions";
    }

    /** Never called with a password here; the OpenID login service asks {@link #getUserIdentity} instead. */
    @Override
    public UserIdentity login(
            String username, Object credentials, Request request, Function<Boolean, Session> session) {
        return null;
    }

    @Override
    public UserIdentity getUserIdentity(Subject subject, Principal principal, boolean create) {
        String[] roles = permissions.permissions(principal.getName()).toArray(String[]::new);
        return Objects.requireNonNull(
                        identityService, "the security handler sets the identity service before any login")
                .newUserIdentity(subject, principal, roles);
    }

    @Override
    public boolean validate(UserIdentity user) {
        return true;
    }

    @Override
    public IdentityService getIdentityService() {
        return identityService;
    }

    @Override
    public void setIdentityService(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void logout(UserIdentity user) {
        // nothing is kept here
    }
}
