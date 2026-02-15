package com.druvu.web.api.handlers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.api.config.UrlHandler;

/**
 * @author Deniss Larka
 * on 22 September 2023
 */
public interface WebSocketHandler extends UrlHandler {

	interface Sessions {

		Set<Session> all();

	}

	interface Session {

		String id();

		void send(Map<String, String> map);

		void close();

		boolean isOpen();

		HttpRequest request();

		Optional<AuthUserIdentity> user();

		void attribute(String key, Object value);

		<T> Optional<T> attribute(String key);

	}

	default void onConnect(Session session, Sessions sessions) {
		// no op
	}

	default void onClose(Session session, Sessions sessions) {
		// no op
	}

	void handle(Session session, Sessions sessions, Map<String, String> message);

}
