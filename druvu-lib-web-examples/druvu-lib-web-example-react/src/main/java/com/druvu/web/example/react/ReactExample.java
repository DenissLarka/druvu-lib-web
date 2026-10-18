package com.druvu.web.example.react;

import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.WebBoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A React app on druvu-lib-web, served by the library's stock parts and nothing written for single-page apps: Vite's
 * build lands in {@code webapp} on the classpath, two static paths serve its files, and one handler answers the root.
 * Start it and open {@code http://localhost:8083/}.
 *
 * @author Deniss Larka <br>
 *     on 18 Oct 2026
 */
public final class ReactExample {

    public static final int PORT = 8083;
    private static final Logger LOG = LoggerFactory.getLogger(ReactExample.class);

    private ReactExample() {}

    static void main(String[] args) {
        WebBoot boot = new WebBoot(config(PORT));
        boot.start("/");
        LOG.info("The React app: http://localhost:{}/", boot.port());
    }

    /** The configuration, with the port open so a test can boot it on a free one. */
    static WebConfig config(int port) {
        return WebConfig.builder()
                .port(port)
                // Vite's output: the page, and the bundles it names, each with a content hash in its file name
                .staticPath("/index.html")
                .staticPath("/assets/*")
                // The first route also answers the context root
                .route(Route.forEveryone(Home.class))
                .build();
    }
}
