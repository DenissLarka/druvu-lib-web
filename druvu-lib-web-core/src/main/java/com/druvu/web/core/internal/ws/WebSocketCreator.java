package com.druvu.web.core.internal.ws;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.api.Session;

import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.WebSocketHandler;
import com.druvu.web.core.handlers.HttpCall;
import com.druvu.web.core.internal.ContextVars;
import com.druvu.web.core.internal.HandlerUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Creating web socket wrapper at connection
 *
 * @author Deniss Larka
 * on 21 September 2023
 */
@Slf4j
public class WebSocketCreator implements JettyWebSocketCreator {

	@Override
	public Object createWebSocket(JettyServerUpgradeRequest upRequest, JettyServerUpgradeResponse upResponse) {

		final Optional<HttpCall> callOpt = HandlerUtils.process(upRequest, upResponse);

		if (callOpt.isPresent()) {
			final HttpCall call = callOpt.get();
			final Set<WebSocketHandler.Session> openedSessions = ContextVars.socketSessions(upRequest.getHttpServletRequest().getServletContext());
			return create(HandlerUtils.handler(call.request().globalAttributes(), call.request().mainPath()), call.request(), openedSessions);
		}
		try {
			if (!upResponse.isCommitted()) {
				upResponse.sendForbidden("Forbidden");
			}
		}
		catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public Session.Listener create(WebSocketHandler handler, HttpRequest request, Set<WebSocketHandler.Session> openedSessions) {
		return new WebSocketWrapper(handler, request, openedSessions);
	}

}
