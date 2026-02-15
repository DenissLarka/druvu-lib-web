package com.druvu.web.core.handlers.attr;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.api.handlers.GlobalAttributes;

import jakarta.servlet.ServletContext;

/**
 *
 * @author : Deniss Larka
 * on 09 June 2024
 **/
public class GlobalAttributesImpl extends Attributes implements GlobalAttributes {

	public static final String HANDLERS = "http_handlers";
	public static final String HTTP_DEFAULT = "http_default_path";
	public static final String AUTH_CONFIG = "auth_config";

	public GlobalAttributesImpl(AttributesBackend backend) {
		super(backend);
	}

	public static GlobalAttributesImpl from(ServletContext context) {
		return new GlobalAttributesImpl(new AttributesBackend.ServletContextBackend(context));
	}

	@Override
	public final Map<String, UrlConfig> handlers() {
		return map(HANDLERS);
	}

	@Override
	public String defaultPath() {
		return string(HTTP_DEFAULT);
	}

	public AuthConfig authConfig(ServletContext context) {
		final AuthConfig authConfig = (AuthConfig) context.getAttribute(AUTH_CONFIG);
		return Objects.requireNonNull(authConfig, "AuthConfig is not provided");
	}

	@Override
	public Set<String> permissionsFor(String matchPath) {
		final UrlConfig urlConfig = handlers().get(matchPath);
		return urlConfig == null ? Set.of() : urlConfig.permissions();
	}

}
