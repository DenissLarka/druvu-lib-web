package com.druvu.web.api.handlers;

import com.druvu.web.api.config.Route;
import java.util.Map;
import java.util.Set;

/**
 * What every handler may reach beyond its own request: the application's global objects, the routes and the default
 * path. A handler gets it from {@code request.globalAttributes()}; the core module provides the implementation.
 *
 * @author Deniss Larka <br>
 *     on 09 Jun 2024
 */
public interface GlobalAttributes {

    /**
     * A global object by name, as given to {@code WebConfig.builder().globalObject(name, value)}.
     *
     * @param key the name
     * @param <C> the expected type
     * @throws IllegalStateException when there is no object of that name
     */
    <C> C get(String key);

    /** Whether a global object of that name exists. */
    boolean has(String key);

    /** The routes by URL. */
    Map<String, Route<?>> routes();

    /** The URL of the first route, which the context root renders. */
    String defaultPath();

    /** The permissions a route requires, or none for an unknown path. */
    Set<String> permissionsFor(String matchPath);
}
