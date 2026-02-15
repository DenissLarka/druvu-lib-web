package com.druvu.web.core.internal.ws;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.WebSocketHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.EqualsAndHashCode;

/**
 * @author Deniss Larka
 * on 22 September 2023
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WebSocketSessionImpl implements WebSocketHandler.Session {

	private static final Gson GSON = new GsonBuilder()
			.serializeNulls()
			.setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
			.setPrettyPrinting()
			.serializeSpecialFloatingPointValues()
			.create();

	private static final ByteBuffer PING = ByteBuffer.wrap("PING".getBytes(StandardCharsets.UTF_8));

	@EqualsAndHashCode.Include
	private final UUID id;
	private final Session sess;
	private final HttpRequest request;
	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	public WebSocketSessionImpl(Session sess, HttpRequest request) {
		this.id = UUID.randomUUID();
		this.sess = Objects.requireNonNull(sess);
		this.request = Objects.requireNonNull(request);
	}


	@Override
	public String id() {
		return id.toString();
	}

	@Override
	public void send(Map<String, String> map) {
		Objects.requireNonNull(map);
		final String json = GSON.toJson(map);
		sess.sendText(json, Callback.NOOP);
	}

	@Override
	public void close() {
		sess.close(org.eclipse.jetty.websocket.api.StatusCode.NORMAL, "Closed by server", Callback.NOOP);
	}

	@Override
	public boolean isOpen() {
		return sess.isOpen();
	}

	@Override
	public HttpRequest request() {
		return request;
	}

	@Override
	public Optional<AuthUserIdentity> user() {
		return request.user();
	}

	@Override
	public void attribute(String key, Object value) {
		Objects.requireNonNull(key);
		attributes.put(key, value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<T> attribute(String key) {
		Objects.requireNonNull(key);
		return Optional.ofNullable((T) attributes.get(key));
	}

	void ping() {
		sess.sendPing(PING, Callback.NOOP);
	}

	public void demand() {
		sess.demand();
	}

	@Override
	public String toString() {
		return "WS/" + id.toString().substring(0, 8) + "/" + sess.getRemoteSocketAddress();
	}
}
