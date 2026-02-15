package com.druvu.web.api.auth;

import java.security.Principal;
import java.util.Set;

/**
 * Interface for authenticated user identity.
 * <p>
 * Provides access to the user's principal and permissions.
 *
 * @author : Deniss Larka
 * <br/>on 05 May 2024
 **/
public interface AuthUserIdentity {

	/**
	 * @return the user's principal
	 */
	Principal getUserPrincipal();

	/**
	 * @return the set of permissions assigned to this user
	 */
	Set<String> getPermissions();
}
