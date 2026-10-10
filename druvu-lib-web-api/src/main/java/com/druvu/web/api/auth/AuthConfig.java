package com.druvu.web.api.auth;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Everything an application decides about signing in: how people do it, what they may do once in, and how long a
 * session lasts.
 *
 * <pre>{@code
 * AuthConfig.builder()                         // a demo: inline users
 *     .basicAuth()
 *     .user("admin", "secret", "orders:read")
 *     .build();
 *
 * AuthConfig.builder()                         // an application: a store of its own
 *     .people(new BasicAuthentication("Shop", users))
 *     .permissions(subject -> repository.permissionsOf(subject))
 *     .sessionTimeout(Duration.ofHours(8))
 *     .machines(new BearerAuthentication(presented -> repository.subjectOf(ApiTokens.hash(presented))))
 *     .build();
 * }</pre>
 *
 * <p>A {@code WebConfig} without an {@code AuthConfig} serves every route to everyone.
 *
 * @author Deniss Larka
 */
public final class AuthConfig {

    public static final Duration DEFAULT_SESSION_TIMEOUT = Duration.ofMinutes(30);
    public static final String DEFAULT_COOKIE_NAME = "session";
    public static final String DEFAULT_REALM = "Application Access";

    private final Authentication people;
    private final PermissionStore permissions;
    private final BearerAuthentication machines;
    private final Duration sessionTimeout;
    private final String cookieName;

    private AuthConfig(
            Authentication people,
            PermissionStore permissions,
            BearerAuthentication machines,
            Duration sessionTimeout,
            String cookieName) {
        this.people = Objects.requireNonNull(people, "people");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
        this.machines = machines;
        this.sessionTimeout = Objects.requireNonNull(sessionTimeout, "sessionTimeout");
        this.cookieName = Objects.requireNonNull(cookieName, "cookieName");
    }

    public static AuthConfigBuilder builder() {
        return new AuthConfigBuilder();
    }

    /** How people sign in. */
    public Authentication people() {
        return people;
    }

    /** What a signed-in subject may do. */
    public PermissionStore permissions() {
        return permissions;
    }

    /** How machines sign in, when the application has routes for them. */
    public Optional<BearerAuthentication> machines() {
        return Optional.ofNullable(machines);
    }

    /** How long a session may stay idle before it ends. */
    public Duration sessionTimeout() {
        return sessionTimeout;
    }

    /** The name of the session cookie. */
    public String cookieName() {
        return cookieName;
    }

    public static final class AuthConfigBuilder {
        private Authentication people;
        private PermissionStore permissions;
        private BearerAuthentication machines;
        private Duration sessionTimeout = DEFAULT_SESSION_TIMEOUT;
        private String cookieName = DEFAULT_COOKIE_NAME;
        private String realm = DEFAULT_REALM;
        private final Map<String, BasicUser> inlineUsers = new LinkedHashMap<>();

        private AuthConfigBuilder() {}

        public AuthConfigBuilder people(Authentication people) {
            this.people = Objects.requireNonNull(people, "people");
            return this;
        }

        public AuthConfigBuilder permissions(PermissionStore permissions) {
            this.permissions = Objects.requireNonNull(permissions, "permissions");
            return this;
        }

        public AuthConfigBuilder machines(BearerAuthentication machines) {
            this.machines = Objects.requireNonNull(machines, "machines");
            return this;
        }

        public AuthConfigBuilder sessionTimeout(Duration sessionTimeout) {
            this.sessionTimeout = Objects.requireNonNull(sessionTimeout, "sessionTimeout");
            return this;
        }

        public AuthConfigBuilder cookieName(String cookieName) {
            this.cookieName = Objects.requireNonNull(cookieName, "cookieName");
            return this;
        }

        /** Inline users follow; the realm is what the browser's password prompt shows. */
        public AuthConfigBuilder basicAuth() {
            return this;
        }

        public AuthConfigBuilder realm(String realm) {
            this.realm = Objects.requireNonNull(realm, "realm");
            return this;
        }

        /** An inline user for Basic authentication, with what the user may do. */
        public AuthConfigBuilder user(String name, String password, String... permissions) {
            inlineUsers.put(Objects.requireNonNull(name, "name"), new BasicUser(password, Set.of(permissions)));
            return this;
        }

        /** @throws IllegalStateException when there is no way to sign in: neither {@link #people} nor inline users */
        public AuthConfig build() {
            Authentication how = people;
            PermissionStore what = permissions;
            if (how == null && !inlineUsers.isEmpty()) {
                how = new BasicAuthentication(realm, inlineUsers);
            }
            if (how == null) {
                throw new IllegalStateException("No way to sign in: give people(...) or at least one user(...)");
            }
            if (what == null) {
                what = how instanceof BasicAuthentication basic ? inlinePermissions(basic) : subject -> Set.of();
            }
            return new AuthConfig(how, what, machines, sessionTimeout, cookieName);
        }

        private static PermissionStore inlinePermissions(BasicAuthentication basic) {
            return subject -> {
                BasicUser user = basic.users().get(subject);
                return user == null ? Set.of() : user.permissions();
            };
        }
    }
}
