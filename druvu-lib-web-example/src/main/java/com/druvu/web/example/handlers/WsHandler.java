package com.druvu.web.example.handlers;

import java.util.Map;

import com.druvu.web.api.handlers.WebSocketHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Deniss Larka
 * on 22 Sep 2023
 */
@Slf4j
public class WsHandler implements WebSocketHandler {

	@Override
	public void onConnect(Session session, Sessions sessions) {
		log.info("WS.onConnect({}) path={} user={} totalSessions={}", session, session.request().mainPath(), session.user().orElse(null), sessions.all().size());
	}

	@Override
	public void onClose(Session session, Sessions sessions) {
		log.info("WS.onClose({}) remainingSessions={}", session, sessions.all().size());
	}

	@Override
	public void handle(Session session, Sessions sessions, Map<String, String> message) {
		log.info("WS.handle({},{})", session, message);
		Thread.ofVirtual().name("WS-handler").start(() -> sendMessage(session));
	}

	private void sendMessage(Session session) {
		try {
			Thread.sleep(2000);
			if (session.isOpen()){
				session.send(Map.of("message", "Hello from WS server!"));
			}
		}
		catch (InterruptedException e) {
			log.warn(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
	}

}
