package com.druvu.web.core.internal;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.http.HttpStatus;

import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.api.config.UrlHandler;
import com.druvu.web.api.handlers.GlobalAttributes;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.druvu.web.api.handlers.PathInfo;
import com.druvu.web.core.auth.SecurityCheck;
import com.druvu.web.core.handlers.HttpCall;
import com.druvu.web.core.handlers.HttpRequestImpl;
import com.druvu.web.core.handlers.HttpResponseImpl;
import com.druvu.web.core.handlers.attr.GlobalAttributesImpl;
import com.druvu.web.core.internal.ws.WebSocketHttpServletResponseDelegate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

/**
 *
 * @author : Deniss Larka
 * on 20 May 2024
 **/
public class HandlerUtils {

	public static Optional<HttpCall> process(JettyServerUpgradeRequest upRequest, JettyServerUpgradeResponse upResponse) {
		final HttpServletRequest httpServletRequest = upRequest.getHttpServletRequest();
		final HttpServletResponse httpServletResponse = new WebSocketHttpServletResponseDelegate(upResponse);
		return doProcess(httpServletRequest, httpServletResponse);
	}

	public static Optional<HttpCall> process(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		return doProcess(httpServletRequest, httpServletResponse);
	}

	private static Optional<HttpCall> doProcess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		final GlobalAttributes globalAttributes = GlobalAttributesImpl.from(httpServletRequest.getServletContext());
		final String mainPath = resolveMainPath(httpServletRequest, globalAttributes);
		final Set<String> requiredPermissions = globalAttributes.permissionsFor(mainPath);

		Optional<AuthUserIdentity> identityOpt;
		if (requiredPermissions.isEmpty()) {
			// Public URL — no authentication needed
			identityOpt = Optional.empty();
		} else {
			// Protected URL — authenticate
			identityOpt = SecurityCheck.remoteUser(httpServletRequest, httpServletResponse);
			if (httpServletResponse.isCommitted()) {
				return Optional.empty();
			}
		}

		HttpRequest req = new HttpRequestImpl(httpServletRequest, identityOpt);
		HttpResponse resp = new HttpResponseImpl(httpServletResponse);

		Set<String> userPermissions = identityOpt.map(SecurityCheck::permissions).orElse(Set.of());
		if (userPermissions.containsAll(requiredPermissions)) {
			return Optional.of(new HttpCall(req, resp));
		}
		if (!resp.isCommitted()) {
			resp.sendError(HttpStatus.UNAUTHORIZED_401);
		}
		return Optional.empty();
	}

	@SneakyThrows
	public static <H extends UrlHandler> H handler(GlobalAttributes context, String matchPath) {
		final UrlConfig urlConfig = context.handlers().get(matchPath);
		final Class<H> handlerClass = urlConfig.urlHandlerClass();
		Objects.requireNonNull(handlerClass);
		return handlerClass.getDeclaredConstructor().newInstance();
	}

	private static String resolveMainPath(HttpServletRequest request, GlobalAttributes globalAttributes) {
		PathInfo pathInfo = new PathInfo(request.getContextPath(), request.getPathInfo(), globalAttributes.defaultPath());
		return pathInfo.mainPath();
	}

}
