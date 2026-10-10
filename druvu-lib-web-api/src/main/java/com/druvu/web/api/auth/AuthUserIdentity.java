package com.druvu.web.api.auth;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

/**
 * The signed-in user as a handler sees it.
 *
 * <p>The subject is the stable id the sign-in gave us: the user name under Basic authentication, the provider's
 * {@code sub} claim under OpenID Connect. Email and display name are there when the sign-in carried them and empty
 * otherwise, so a handler can greet a person without knowing how they arrived.
 *
 * @author Deniss Larka
 */
public interface AuthUserIdentity {

    /** The stable id of the signed-in user, which is what an application keys its own records on. */
    String subject();

    /** The subject as a principal, for APIs that want one; its name is {@link #subject()}. */
    Principal getUserPrincipal();

    Set<String> getPermissions();

    /** The user's email address when the sign-in carried one. */
    Optional<String> email();

    /** Something to greet the user with when the sign-in carried a name. */
    Optional<String> displayName();
}
