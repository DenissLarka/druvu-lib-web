package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.WebSocketHandler;
import java.util.Map;

/** A WebSocket at {@code /echo-socket} that answers every message with its text and the caller's user name. */
public class EchoSocketHandler implements WebSocketHandler {
    @Override
    public void handle(Session session, Sessions sessions, Map<String, String> message) {
        String user = session.user()
                .map(identity -> identity.getUserPrincipal().getName())
                .orElse("anonymous");
        session.send(Map.of("echo", message.getOrDefault("text", ""), "user", user));
    }
}
