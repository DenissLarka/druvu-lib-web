package com.druvu.web.api.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.druvu.web.api.utils.HandlerNameConvention;

/**
 *
 * @author : Deniss Larka
 * on 21 April 2024
 **/
public record UrlConfig<T extends UrlHandler>(String url, Class<T> urlHandlerClass, boolean _default, Set<String> permissions) {

	public UrlConfig {
		Objects.requireNonNull(url);
		Objects.requireNonNull(urlHandlerClass);
		permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
	}

	@Override
	public Set<String> permissions() {
		return Collections.unmodifiableSet(permissions);
	}

	/**
	 * Create a UrlConfig with permissions. Empty permissions means publicly accessible.
	 *
	 * @param urlHandlerClass the handler class
	 * @param permissions     required permissions (omit for public access)
	 */
	public static <T extends UrlHandler> UrlConfig<T> from(Class<T> urlHandlerClass, String... permissions) {
		return new UrlConfig<>(HandlerNameConvention.translate(urlHandlerClass), urlHandlerClass, false, toSet(permissions));
	}

	public static <T extends UrlHandler> UrlConfig<T> from(Class<T> urlHandlerClass, boolean _default, String... permissions) {
		return new UrlConfig<>(HandlerNameConvention.translate(urlHandlerClass), urlHandlerClass, _default, toSet(permissions));
	}

	private static Set<String> toSet(String[] permissions) {
		if (permissions == null || permissions.length == 0) {
			return Set.of();
		}
		return new HashSet<>(Arrays.asList(permissions));
	}
}
