package com.druvu.web.api.config;

import com.druvu.web.api.auth.AuthConfig;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Everything the server needs: where to listen, the routes, where static files come from, the objects every handler may
 * reach, and how people and machines sign in.
 *
 * <pre>{@code
 * WebConfig.builder()
 *     .port(8080)
 *     .route(Route.forEveryone(HomeHandler.class))     // the first route also answers the context root
 *     .route(Route.forSignedIn(AccountHandler.class))
 *     .authConfig(auth)                                // optional: without it, every route is public
 *     .build();
 * }</pre>
 *
 * @author Deniss Larka <br>
 *     on 21 Apr 2024
 */
public final class WebConfig {

    /** The port when none is given. */
    public static final int DEFAULT_PORT = 8080;

    /** Always served as static files: WebJars and the application's {@code webapp/static}. */
    public static final Set<String> DEFAULT_STATIC_PATHS = Set.of("/webjars/*", "/static/*");

    private final String host;
    private final int port;
    private final Path serveFromDirectory;
    private final Set<String> staticPaths;
    private final Map<String, Object> globalObjects;
    private final List<Route<?>> routes;
    private final AuthConfig authConfig;

    private WebConfig(WebConfigBuilder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.serveFromDirectory = builder.serveFromDirectory;
        this.staticPaths = Set.copyOf(builder.staticPaths);
        this.globalObjects = Map.copyOf(builder.globalObjects);
        this.routes = List.copyOf(builder.routes);
        this.authConfig = builder.authConfig;
    }

    public static WebConfigBuilder builder() {
        return new WebConfigBuilder();
    }

    /** The address to bind, or empty for the loopback address only. */
    public Optional<String> host() {
        return Optional.ofNullable(host);
    }

    /** The port to listen on; 0 means a free one, which {@code WebBoot.port()} reports once started. */
    public int port() {
        return port;
    }

    /** A directory on disk served as static files, in front of the classpath's {@code webapp}. */
    public Optional<Path> serveFromDirectory() {
        return Optional.ofNullable(serveFromDirectory);
    }

    /** The URL patterns served as static files: the defaults plus the application's own. */
    public Set<String> staticPaths() {
        return staticPaths;
    }

    /** Objects every handler and template may reach by name, through {@code request.globalAttributes()}. */
    public Map<String, Object> globalObjects() {
        return globalObjects;
    }

    /** The routes in the order given; the first answers the context root as well. */
    public List<Route<?>> routes() {
        return routes;
    }

    /** How people and machines sign in, or empty when every route is public. */
    public Optional<AuthConfig> authConfig() {
        return Optional.ofNullable(authConfig);
    }

    public static final class WebConfigBuilder {
        private String host;
        private int port = DEFAULT_PORT;
        private Path serveFromDirectory;
        private final Set<String> staticPaths = new LinkedHashSet<>(DEFAULT_STATIC_PATHS);
        private final Map<String, Object> globalObjects = new LinkedHashMap<>();
        private final List<Route<?>> routes = new ArrayList<>();
        private AuthConfig authConfig;

        private WebConfigBuilder() {}

        /** The address to bind, {@code 0.0.0.0} for every interface. Without it the server listens on loopback only. */
        public WebConfigBuilder host(String host) {
            this.host = Objects.requireNonNull(host, "host");
            return this;
        }

        /** The port to listen on, 0 for a free one; {@value #DEFAULT_PORT} without it. */
        public WebConfigBuilder port(int port) {
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("A port is 0 to 65535: " + port);
            }
            this.port = port;
            return this;
        }

        /** A directory on disk served as static files, in front of the classpath's {@code webapp}. */
        public WebConfigBuilder serveFromDirectory(Path directory) {
            this.serveFromDirectory = Objects.requireNonNull(directory, "directory");
            return this;
        }

        /** One more URL pattern served as static files, {@code /assets/*} say; the defaults stay. */
        public WebConfigBuilder staticPath(String pattern) {
            Objects.requireNonNull(pattern, "pattern");
            if (!pattern.startsWith("/")) {
                throw new IllegalArgumentException("A static path starts with '/': " + pattern);
            }
            staticPaths.add(pattern);
            return this;
        }

        /** An object every handler and template may reach under this name. */
        public WebConfigBuilder globalObject(String name, Object value) {
            globalObjects.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** One more route; the first one given also answers the context root. */
        public WebConfigBuilder route(Route<?> route) {
            routes.add(Objects.requireNonNull(route, "route"));
            return this;
        }

        /** More routes, in their order. */
        public WebConfigBuilder routes(Collection<? extends Route<?>> routes) {
            routes.forEach(this::route);
            return this;
        }

        /** How people and machines sign in. Without it, every route is public. */
        public WebConfigBuilder authConfig(AuthConfig authConfig) {
            this.authConfig = Objects.requireNonNull(authConfig, "authConfig");
            return this;
        }

        /** @throws IllegalStateException without a route, or with two routes on one URL */
        public WebConfig build() {
            if (routes.isEmpty()) {
                throw new IllegalStateException(
                        "No routes: give at least one route(...); the first answers the context root");
            }
            Map<String, Route<?>> byUrl = new LinkedHashMap<>();
            for (Route<?> route : routes) {
                Route<?> other = byUrl.putIfAbsent(route.url(), route);
                if (other != null) {
                    throw new IllegalStateException("Two routes on /" + route.url() + ": "
                            + other.handlerClass().getName() + " and "
                            + route.handlerClass().getName());
                }
            }
            return new WebConfig(this);
        }
    }
}
