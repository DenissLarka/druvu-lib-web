package com.druvu.web.core.test.wiring;

import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.WebBoot;
import java.net.URI;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/** Boots a {@link WebBoot} on an ephemeral port for a test class and hands out URIs under its context path. */
final class BootedApp implements AutoCloseable {
    private final Server server;
    private final String contextPath;

    private BootedApp(Server server, String contextPath) {
        this.server = server;
        this.contextPath = contextPath;
    }

    static BootedApp start(WebConfig config, String contextPath) {
        return new BootedApp((Server) new WebBoot(config).start(contextPath), contextPath);
    }

    int port() {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port() + contextPath + path);
    }

    URI wsUri(String path) {
        return URI.create("ws://127.0.0.1:" + port() + contextPath + path);
    }

    @Override
    public void close() throws Exception {
        server.stop();
    }
}
