package com.druvu.web.core;

import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.internal.ConnectorsInstaller;
import com.druvu.web.core.internal.ContextCreator;
import com.druvu.web.core.internal.ContextVars;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * The server: built from a {@link WebConfig}, started under a context path, stopped by {@link #stop()} or with the JVM.
 *
 * <pre>{@code
 * WebBoot boot = new WebBoot(config);
 * boot.start("/app");        // http://localhost:8080/app/...
 * boot.port();               // the port bound, which matters when the configuration said 0
 * boot.stop();               // optional: the JVM's shutdown stops it too
 * }</pre>
 *
 * @author Deniss Larka <br>
 *     on 21 Apr 2024
 */
public final class WebBoot {

    private final WebConfig config;
    private Server server;

    public WebBoot(WebConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Starts the server under the context path, {@code /app} say, and returns once it listens.
     *
     * @throws IllegalArgumentException for a context path that does not start with {@code /}
     * @throws IllegalStateException when already started, or when the server cannot start
     */
    public synchronized void start(String contextPath) {
        if (contextPath == null || !contextPath.startsWith("/")) {
            throw new IllegalArgumentException("A context path starts with '/': " + contextPath);
        }
        if (server != null) {
            throw new IllegalStateException("Already started");
        }
        Server candidate = new Server();
        candidate.setStopAtShutdown(true);
        ConnectorsInstaller.install(config, candidate);
        ServletContextHandler handler = ContextCreator.create(config, contextPath);
        handler.addEventListener(new Listener());
        candidate.setHandler(handler);
        try {
            candidate.start();
        } catch (Exception e) {
            throw new IllegalStateException("The server could not start on port " + config.port(), e);
        }
        server = candidate;
    }

    /**
     * The port the server listens on: the one bound, which is the news when the configuration said 0.
     *
     * @throws IllegalStateException when not started
     */
    public synchronized int port() {
        if (server == null) {
            throw new IllegalStateException("Not started");
        }
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    /** Stops the server and returns once it has; nothing happens when it is not running. */
    public synchronized void stop() {
        if (server == null) {
            return;
        }
        try {
            server.stop();
        } catch (Exception e) {
            throw new IllegalStateException("The server did not stop cleanly", e);
        } finally {
            server = null;
        }
    }

    private final class Listener implements ServletContextListener {

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            final ServletContext context = sce.getServletContext();
            for (Map.Entry<String, Object> entry : config.globalObjects().entrySet()) {
                context.setAttribute(entry.getKey(), entry.getValue());
            }
            context.setAttribute(ContextVars.HANDLERS, routesByUrl());
            context.setAttribute(
                    ContextVars.HTTP_DEFAULT, config.routes().getFirst().url());
            config.authConfig().ifPresent(auth -> context.setAttribute(ContextVars.AUTH_CONFIG, auth));
            context.setAttribute(ContextVars.WS_OPENED, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        }

        private Map<String, Route<?>> routesByUrl() {
            return config.routes().stream().collect(Collectors.toMap(Route::url, Function.identity()));
        }
    }
}
