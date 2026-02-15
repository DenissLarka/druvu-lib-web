package com.druvu.web.api.handlers;

import com.druvu.web.api.auth.AuthUserIdentity;
import jakarta.servlet.DispatcherType;

import java.util.Optional;

/**
 * Interface for HTTP request abstraction.
 * <p>
 * Provides access to request path, parameters, attributes, and user identity.
 * Implementations are created via factory methods in the core module.
 *
 * @author : Deniss Larka
 * on 08 June 2024
 **/
public interface HttpRequest {

	void setAttribute(String key, Object value);

	GlobalAttributes globalAttributes();

	PathInfo pathInfo();

	String contentType();

	DispatcherType dispatcherType();

	String method();

	boolean isDispatcherTypeRequest();

	boolean isDispatcherTypeInclude();

	String mainPath();

	ParamInfo paramInfo();

	Object getAttribute(String attributeName);

	Optional<AuthUserIdentity> user();
}
