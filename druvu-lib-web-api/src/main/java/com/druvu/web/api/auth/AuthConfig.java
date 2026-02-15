package com.druvu.web.api.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;

/**
 * Authentication configuration for the web service.
 * <p>
 * Built via {@link #builder()}. Supports both inline user definitions
 * (for development and simple apps) and pluggable {@link UserStore}
 * implementations (for a database, LDAP, etc.).
 * <p>
 * Example with inline users:
 * <pre>{@code
 * AuthConfig.builder()
 *     .basicAuth()
 *     .realm("My App")
 *     .user("admin", "secret", "admin:permission", "generic:permission")
 *     .user("viewer", "pass", "generic:permission")
 *     .build();
 * }</pre>
 * <p>
 * Example with pluggable user store:
 * <pre>{@code
 * AuthConfig.builder()
 *     .basicAuth()
 *     .userStore(myJdbcUserStore)
 *     .build();
 * }</pre>
 *
 * @author Deniss Larka
 * on 11 March 2022
 */
@Getter
public class AuthConfig {

	private final String authType;
	private final String realmName;
	private final boolean sessionRenewal;
	private final int sessionTimeoutSeconds;
	private final UserStore userStore;

	AuthConfig(String authType, String realmName, boolean sessionRenewal,
			   int sessionTimeoutSeconds, UserStore userStore) {
		this.authType = Objects.requireNonNull(authType);
		this.realmName = Objects.requireNonNull(realmName);
		this.sessionRenewal = sessionRenewal;
		this.sessionTimeoutSeconds = sessionTimeoutSeconds;
		this.userStore = Objects.requireNonNull(userStore);
	}

	public static AuthConfigBuilder builder() {
		return new AuthConfigBuilder();
	}

	public static class AuthConfigBuilder {
		private String authType = "BASIC";
		private String realmName = "Application Access";
		private boolean sessionRenewal = true;
		private int sessionTimeoutSeconds = 1800;
		private UserStore userStore;
		private final Map<String, InMemoryUserStore.User> inlineUsers = new LinkedHashMap<>();

		public AuthConfigBuilder authType(String authType) {
			this.authType = authType;
			return this;
		}

		public AuthConfigBuilder basicAuth() {
			this.authType = "BASIC";
			return this;
		}

		public AuthConfigBuilder realm(String realmName) {
			this.realmName = realmName;
			return this;
		}

		public AuthConfigBuilder sessionRenewal(boolean sessionRenewal) {
			this.sessionRenewal = sessionRenewal;
			return this;
		}

		public AuthConfigBuilder sessionTimeout(int seconds) {
			this.sessionTimeoutSeconds = seconds;
			return this;
		}

		/**
		 * Set a custom {@link UserStore} for user permission retrieval.
		 * Mutually exclusive with {@link #user(String, String, String...)}.
		 */
		public AuthConfigBuilder userStore(UserStore userStore) {
			this.userStore = userStore;
			return this;
		}

		/**
		 * Add an in-memory user with credentials and permissions.
		 * Mutually exclusive with {@link #userStore(UserStore)}.
		 *
		 * @param name        the username
		 * @param password    the password
		 * @param permissions permission strings granted to this user
		 */
		public AuthConfigBuilder user(String name, String password, String... permissions) {
			inlineUsers.put(name, new InMemoryUserStore.User(password, Set.of(permissions)));
			return this;
		}

		public AuthConfig build() {
			UserStore store = this.userStore;
			if (store == null) {
				if (inlineUsers.isEmpty()) {
					throw new IllegalStateException("Either userStore() or at least one user() must be provided");
				}
				store = new InMemoryUserStore(inlineUsers);
			}
			return new AuthConfig(authType, realmName, sessionRenewal, sessionTimeoutSeconds, store);
		}
	}
}
