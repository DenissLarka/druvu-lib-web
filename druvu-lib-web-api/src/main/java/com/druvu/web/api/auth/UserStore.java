package com.druvu.web.api.auth;

import java.util.Set;

/**
 * Interface for retrieving user permissions.
 * <p>
 * Implementations provide permission data for authenticated users.
 * The API module includes {@link InMemoryUserStore} for simple use cases.
 * For production, implement this interface to integrate with your user store
 * (database, LDAP, etc.).
 *
 * @author Deniss Larka
 */
public interface UserStore {

	/**
	 * Get the permissions associated with an authenticated user.
	 *
	 * @param principalName the name of the authenticated principal (username)
	 * @return set of permission strings for the principal, or empty set if none
	 */
	Set<String> permissions(String principalName);
}
