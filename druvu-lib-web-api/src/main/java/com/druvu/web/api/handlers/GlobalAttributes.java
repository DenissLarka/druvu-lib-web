package com.druvu.web.api.handlers;

import com.druvu.web.api.config.UrlConfig;

import java.util.Map;
import java.util.Set;

/**
 * Interface for accessing global (application-scoped) attributes.
 * <p>
 * Provides access to handlers, default path, and permissions.
 * The core module provides the implementation.
 *
 * @author : Deniss Larka
 * on 09 June 2024
 **/
public interface GlobalAttributes {

	/**
	 * Get an attribute by key.
	 *
	 * @param key the attribute key
	 * @param <C> the expected type
	 * @return the attribute value
	 * @throws IllegalStateException if the attribute is not found
	 */
	<C> C get(String key);

	/**
	 * Check if an attribute exists.
	 *
	 * @param key the attribute key
	 * @return true if the attribute exists
	 */
	boolean has(String key);

	/**
	 * @return the map of URL path to handler configuration
	 */
	Map<String, UrlConfig> handlers();

	/**
	 * @return the default path to use when path is empty
	 */
	String defaultPath();

	/**
	 * Get the required permissions for a given path.
	 *
	 * @param matchPath the path to check
	 * @return set of required permissions, or empty set if none
	 */
	Set<String> permissionsFor(String matchPath);
}
