package com.druvu.web.core.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.api.handlers.WebSocketHandler;
import com.druvu.web.core.auth.JettyAuthAdapter;

import jakarta.servlet.ServletContext;

/**
 *
 * @author : Deniss Larka
 * <br/>on 21 Apr 2024
 **/
public class ContextVars {

	//Map<String, UrlConfig>
	public static final String HANDLERS = "http_handlers";
	public static final String WS_HANDLERS = "ws_handlers";
	public static final String WS_OPENED = "ws_opened";
	public static final String HTTP_DEFAULT = "http_default_path";
	public static final String HTTP_SECURITY = "http_security";
	public static final String AUTH_CONFIG = "auth_config";
	public static final String TEMPLATE_SYSTEM = "template_system";
	public static final String JETTY_AUTH = "jetty_auth_adapter";

	public static String templateSystem(ServletContext context) {
		return string(context, TEMPLATE_SYSTEM);
	}

	public static final Map<String, UrlConfig> handlers(ServletContext context) {
		return map(context, HANDLERS);
	}

	public static String defaultPath(ServletContext context) {
		return string(context, HTTP_DEFAULT);
	}

	public static AuthConfig authConfig(ServletContext context) {
		final AuthConfig authConfig = (AuthConfig) context.getAttribute(AUTH_CONFIG);
		return Objects.requireNonNull(authConfig, "AuthConfig is not provided");
	}

	public static JettyAuthAdapter jettyAuth(ServletContext context) {
		final JettyAuthAdapter adapter = (JettyAuthAdapter) context.getAttribute(JETTY_AUTH);
		return Objects.requireNonNull(adapter, "JettyAuthAdapter is not provided");
	}

	public static Set<String> permissionsFor(ServletContext context, String matchPath) {
		final UrlConfig urlConfig = handlers(context).get(matchPath);
		return urlConfig == null ? Set.of() : urlConfig.permissions();
	}

	public static Set<WebSocketHandler.Session> socketSessions(ServletContext context) {
		return set(context, WS_OPENED);
	}

	private static <K, V> Map<K, V> map(ServletContext context, String keyName) {
		return (Map<K, V>) Objects.requireNonNull(context.getAttribute(keyName), noValueForKeyError(keyName));
	}

	private static <K> Set<K> set(ServletContext context, String keyName) {
		return (Set<K>) Objects.requireNonNull(context.getAttribute(keyName), noValueForKeyError(keyName));
	}

	private static String string(ServletContext context, String keyName) {
		return (String) Objects.requireNonNull(context.getAttribute(keyName), noValueForKeyError(keyName));
	}

	private static String noValueForKeyError(String keyName) {
		return String.format("No %s in context", keyName);
	}

}
