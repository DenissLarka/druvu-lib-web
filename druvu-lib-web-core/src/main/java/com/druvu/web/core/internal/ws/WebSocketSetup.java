package com.druvu.web.core.internal.ws;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;

import com.druvu.web.api.handlers.WebSocketHandler;
import com.druvu.web.core.internal.ContextVars;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lombok.extern.slf4j.Slf4j;

/**
 * Configure WS during setup
 *
 * @author Deniss Larka
 * on 21 September 2023
 */
@Slf4j
public class WebSocketSetup implements Consumer<ServletContextHandler>, JettyWebSocketServletContainerInitializer.Configurator {

	private static final int MAX_TEXT_MESSAGE_SIZE = 65535;

	private static final int PING_DELAY = 10;

	private final ScheduledExecutorService pingExecutor = Executors.newScheduledThreadPool(1, new WebSocketPingThreadFactory());

	@Override
	public void accept(ServletContextHandler context) {
		JettyWebSocketServletContainerInitializer.configure(context, this);
		context.addEventListener(new PingShutdownListener());
	}

	@Override
	public void accept(ServletContext context, JettyWebSocketServerContainer wsContainer) {
		wsContainer.setMaxTextMessageSize(MAX_TEXT_MESSAGE_SIZE);
		wsContainer.addMapping("/*", new WebSocketCreator());
		pingExecutor.scheduleWithFixedDelay(
				() -> pingAll(context),
				PING_DELAY,
				PING_DELAY,
				TimeUnit.SECONDS);
	}

	private void pingAll(ServletContext context) {
		try {
			ContextVars.socketSessions(context).forEach(this::ping);
		} catch (Exception e) {
			log.warn("Ping cycle failed: {}", e.getMessage(), e);
		}
	}

	private void ping(WebSocketHandler.Session session) {
		try {
			if (session instanceof WebSocketSessionImpl socketSession) {
				socketSession.ping();
			}
		} catch (Exception e) {
			log.warn("Ping failed for session {}: {}", session, e.getMessage());
		}
	}

	private class PingShutdownListener implements ServletContextListener {

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			log.info("Shutting down WebSocket ping executor");
			pingExecutor.shutdownNow();
		}
	}
}
