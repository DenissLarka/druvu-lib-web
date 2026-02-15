package com.druvu.web.api.auth;

import java.util.Map;
import java.util.Set;

/**
 * In-memory implementation of {@link UserStore}.
 * <p>
 * Stores users with their credentials and permissions.
 * Suitable for development, testing, and small applications.
 * <p>
 * Typically created via {@link AuthConfig.AuthConfigBuilder#user(String, String, String...)}
 * rather than directly.
 *
 * @author Deniss Larka
 */
public class InMemoryUserStore implements UserStore {

	/**
	 * A user entry with password and permissions.
	 */
	public record User(String password, Set<String> permissions) {
		public User {
			permissions = Set.copyOf(permissions);
		}
	}

	private final Map<String, User> users;

	public InMemoryUserStore(Map<String, User> users) {
		this.users = Map.copyOf(users);
	}

	@Override
	public Set<String> permissions(String principalName) {
		User user = users.get(principalName);
		return user != null ? user.permissions() : Set.of();
	}

	/**
	 * @return unmodifiable map of all users (name to user record)
	 */
	public Map<String, User> users() {
		return users;
	}
}
