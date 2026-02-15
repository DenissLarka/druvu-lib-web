package com.druvu.web.core.handlers.attr;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.servlet.ServletContext;


/**
 *
 * @author : Deniss Larka
 * <br/>on 09 June 2024
 */
public class Attributes {

	private final AttributesBackend backend;

	public static GlobalAttributesImpl from(ServletContext context) {
		return new GlobalAttributesImpl(new AttributesBackend.ServletContextBackend(context));
	}

	public Attributes(AttributesBackend backend) {
		this.backend = Objects.requireNonNull(backend);
	}

	public <C> C get(String key) {
		if (backend.getAttribute(key) == null) {
			throw new IllegalStateException("No attribute found:" + key);
		}
		return (C) backend.getAttribute(key);
	}

	public boolean has(String key) {
		return backend.getAttribute(key) != null;
	}

	protected <K, V> Map<K, V> map(String keyName) {
		return Map.copyOf(Objects.requireNonNull(get(keyName), noValueForKeyError(keyName)));
	}

	protected <K> Set<K> set(String keyName) {
		return Set.copyOf(Objects.requireNonNull(get(keyName), noValueForKeyError(keyName)));
	}

	protected String string(String keyName) {
		return Objects.requireNonNull(get(keyName), noValueForKeyError(keyName));
	}

	private static String noValueForKeyError(String keyName) {
		return String.format("No %s in context", keyName);
	}
}
