package com.druvu.web.core.internal;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * @author Deniss Larka
 * on 18 February 2023
 */
public class RequestWrapper extends HttpServletRequestWrapper {

	private final String pathInfo;

	/**
	 * Constructs a request object wrapping the given request.
	 *
	 * @param request  the {@link HttpServletRequest} to be wrapped.
	 * @param pathInfo static value always returned by {@link #getPathInfo()}
	 */
	public RequestWrapper(HttpServletRequest request, String pathInfo) {
		super(request);
		this.pathInfo = Objects.requireNonNull(pathInfo);
	}

	@Override
	public String getPathInfo() {
		return pathInfo;
	}
}
