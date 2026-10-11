package com.druvu.web.core.test.wiring;

import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.WebBoot;
import java.net.URI;

/** Boots a {@link WebBoot} on a free port for a test class and hands out URIs under its context path. */
final class BootedApp implements AutoCloseable {
    private final WebBoot boot;
    private final String contextPath;

    private BootedApp(WebBoot boot, String contextPath) {
        this.boot = boot;
        this.contextPath = contextPath;
    }

    static BootedApp start(WebConfig config, String contextPath) {
        WebBoot boot = new WebBoot(config);
        boot.start(contextPath);
        return new BootedApp(boot, contextPath);
    }

    int port() {
        return boot.port();
    }

    URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port() + contextPath + path);
    }

    URI wsUri(String path) {
        return URI.create("ws://127.0.0.1:" + port() + contextPath + path);
    }

    @Override
    public void close() {
        boot.stop();
    }
}
