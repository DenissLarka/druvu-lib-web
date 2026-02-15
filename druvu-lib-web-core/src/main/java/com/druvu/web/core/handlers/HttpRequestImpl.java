package com.druvu.web.core.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.UserIdentity;

import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.api.handlers.GlobalAttributes;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.ParamInfo;
import com.druvu.web.api.handlers.PathInfo;
import com.druvu.web.core.handlers.attr.GlobalAttributesImpl;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : Deniss Larka
 * on 08 June 2024
 **/
@Slf4j
public final class HttpRequestImpl implements HttpRequest {

	private static final String ANONYMOUS = "ANONYMOUS";

	private final Optional<AuthUserIdentity> authUserOpt;
	private final PathInfo pathInfo;
	private final ParamInfo paramInfo;
	private final DispatcherType dispatcherType;
	private final GlobalAttributes globalAttributes;
	private final HttpServletRequest request;

	public HttpRequestImpl(HttpServletRequest request, Optional<AuthUserIdentity> identityOpt) {
		this.request = Objects.requireNonNull(request);
		this.authUserOpt = Objects.requireNonNull(identityOpt);
		this.globalAttributes = GlobalAttributesImpl.from(request.getServletContext());
		this.pathInfo = new PathInfo(request.getContextPath(), request.getPathInfo(), globalAttributes.defaultPath());
		this.paramInfo = paramInfo(request);
		this.dispatcherType = request.getDispatcherType();
	}

	private ParamInfo paramInfo(HttpServletRequest req) {
		Map<String, String[]> parameterMap = new HashMap<>();
		parameterMap.putAll(req.getParameterMap());
		/*
		 * Multipart form data
		 */
		final String contentType = req.getContentType();
		if (contentType != null && MimeTypes.Type.MULTIPART_FORM_DATA.is(contentType)) {
			try {
				for (Part part : req.getParts()) {
					try (InputStreamReader streamReader = new InputStreamReader(part.getInputStream(), StandardCharsets.UTF_8)) {
						try (BufferedReader bufferedReader = new BufferedReader(streamReader)) {
							final String value = bufferedReader.lines().collect(Collectors.joining("\n"));
							parameterMap.put(part.getName(), new String[] {value});
						}
					}
				}
			}
			catch (ServletException e) {
				log.error(e.getMessage(), e);
			}
			catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		return new ParamInfo(parameterMap);
	}

	@Override
	public void setAttribute(String key, Object value) {
		request.setAttribute(key, value);
	}

	@Override
	public GlobalAttributes globalAttributes() {
		return globalAttributes;
	}

	@Override
	public PathInfo pathInfo() {
		return pathInfo;
	}

	@Override
	public String contentType() {
		return request.getContentType();
	}

	@Override
	public DispatcherType dispatcherType() {
		return dispatcherType;
	}

	@Override
	public String method() {
		return request.getMethod();
	}

	@Override
	public boolean isDispatcherTypeRequest() {
		return dispatcherType() == DispatcherType.REQUEST;
	}

	@Override
	public boolean isDispatcherTypeInclude() {
		return dispatcherType() == DispatcherType.INCLUDE;
	}

	@Override
	public String mainPath() {
		return pathInfo.mainPath();
	}

	@Override
	public ParamInfo paramInfo() {
		return paramInfo;
	}

	@Override
	public Object getAttribute(String attributeName) {
		return request.getAttribute(attributeName);
	}

	public Optional<AuthUserIdentity> user() {
		return authUserOpt;
	}

	public String userName() {
		return user()
				.map(AuthUserIdentity::getUserPrincipal)
				.map(Principal::getName)
				.orElse(ANONYMOUS);
	}

	@Override
	public String toString() {
		return pathInfo().toString() + paramInfo().toString() + '@' + userName();
	}
}
