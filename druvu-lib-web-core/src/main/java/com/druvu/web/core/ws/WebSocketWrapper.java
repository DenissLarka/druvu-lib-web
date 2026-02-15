package com.druvu.web.core.ws;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.WebSocketHandler;
import com.druvu.web.core.internal.ws.WebSocketSessionImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import lombok.extern.slf4j.Slf4j;

/**
 * Handle WS connections and keep active sessions
 * Convert text messages to JSON object
 *
 * @author Deniss Larka
 * on 22 September 2023
 */
@Slf4j
public class WebSocketWrapper implements Session.Listener {

	private final static Gson GSON = new GsonBuilder()
			.serializeNulls()
			.setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
			.setPrettyPrinting()
			.serializeSpecialFloatingPointValues()
			.create();

	private final static Type MAP_STR_TYPE = new TypeToken<Map<String, String>>() {
	}.getType();

	private final WebSocketHandler handler;
	private final HttpRequest request;
	private final Set<WebSocketHandler.Session> openedSessions;
	private final WebSocketHandler.Sessions sessions;
	private final AtomicReference<WebSocketSessionImpl> socketSession = new AtomicReference<>();

	public WebSocketWrapper(WebSocketHandler handler, HttpRequest request, Set<WebSocketHandler.Session> openedSessions) {
		this.handler = Objects.requireNonNull(handler);
		this.request = Objects.requireNonNull(request);
		this.openedSessions = Objects.requireNonNull(openedSessions);
		this.sessions = () -> Collections.unmodifiableSet(openedSessions);
	}

	@Override
	public void onWebSocketOpen(Session sess) {
		try {
			if (this.socketSession.get() != null) {
				throw new IllegalStateException("Session already exist");
			}
			this.socketSession.set(new WebSocketSessionImpl(sess, request));
			openedSessions.add(socketSession.get());
			handler.onConnect(socketSession.get(), sessions);
			socketSession.get().demand();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	@Override
	public void onWebSocketText(String message) {
		try {
			final Map<String, String> map = toMap(message);
			if (!map.isEmpty()) {
				handler.handle(socketSession.get(), sessions, map);
			}
			socketSession.get().demand();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	@Override
	public void onWebSocketClose(int statusCode, String reason, Callback callback) {
		try {
			openedSessions.remove(socketSession.get());
			handler.onClose(socketSession.get(), sessions);
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		final WebSocketSessionImpl sess = socketSession.get();
		openedSessions.remove(sess);
		if (log.isDebugEnabled()) {
			log.debug("WebSocket error for session {}: {}", sess, cause.getMessage(), cause);
		}
	}

	private Map<String, String> toMap(String message) {

		try {
			return GSON.fromJson(message, MAP_STR_TYPE);
		}
		catch (JsonParseException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage(), e);
			}
			log.warn("Unrecognized message:{}", message);
		}
		return Collections.emptyMap();
	}
}
