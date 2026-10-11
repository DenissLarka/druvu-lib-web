package com.druvu.web.core.internal;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.config.Route;
import com.druvu.web.api.handlers.WebSocketHandler;
import jakarta.servlet.ServletContext;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The names under which the boot puts things on the servlet context, and typed ways to read them back.
 *
 * @author Deniss Larka
 */
public class ContextVars {

    // Map<String, Route<?>>
    public static final String HANDLERS = "http_handlers";
    public static final String WS_HANDLERS = "ws_handlers";
    public static final String WS_OPENED = "ws_opened";
    public static final String HTTP_DEFAULT = "http_default_path";
    public static final String AUTH_CONFIG = "auth_config";

    @SuppressWarnings("unchecked")
    public static Map<String, Route<?>> routes(ServletContext context) {
        return (Map<String, Route<?>>) required(context, HANDLERS);
    }

    public static String defaultPath(ServletContext context) {
        return (String) required(context, HTTP_DEFAULT);
    }

    /** The application's sign-in configuration, or empty when it serves everything to everyone. */
    public static Optional<AuthConfig> authConfig(ServletContext context) {
        return Optional.ofNullable((AuthConfig) context.getAttribute(AUTH_CONFIG));
    }

    public static Set<String> permissionsFor(ServletContext context, String matchPath) {
        final Route<?> route = routes(context).get(matchPath);
        return route == null ? Set.of() : route.permissions();
    }

    @SuppressWarnings("unchecked")
    public static Set<WebSocketHandler.Session> socketSessions(ServletContext context) {
        return (Set<WebSocketHandler.Session>) required(context, WS_OPENED);
    }

    private static Object required(ServletContext context, String keyName) {
        return Objects.requireNonNull(context.getAttribute(keyName), () -> "No " + keyName + " in context");
    }
}
