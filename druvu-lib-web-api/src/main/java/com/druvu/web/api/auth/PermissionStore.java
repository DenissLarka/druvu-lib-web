package com.druvu.web.api.auth;

import java.util.Set;

/**
 * What a signed-in subject may do. The application owns the answer, a database row usually.
 *
 * <p>Asked once, when the subject signs in; the answer then lives in the session next to the identity, so a store
 * backed by a database is not hit on every request. A subject the store does not know gets an empty set: signed in,
 * allowed nothing that needs a permission.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
@FunctionalInterface
public interface PermissionStore {

    /** The permissions of this subject, empty when there are none or the subject is unknown; never null. */
    Set<String> permissions(String subject);
}
