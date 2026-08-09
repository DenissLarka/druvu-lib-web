package com.druvu.web.core.internal;

import com.druvu.web.api.config.WebConfig;
import java.net.InetAddress;
import lombok.NonNull;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * @author : Deniss Larka <br>
 *     on 21 Apr 2024
 */
public class ConnectorsInstaller {

    public static void install(@NonNull WebConfig webConfig, Server server) {
        server.setConnectors(new ServerConnector[] {connector(webConfig, server, httpConfig())});
    }

    private static ServerConnector connector(WebConfig webConfig, Server server, HttpConfiguration httpConfig) {
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        connector.setPort(webConfig.port());
        connector.setHost(
                webConfig.host() == null ? InetAddress.getLoopbackAddress().getHostAddress() : webConfig.host());
        return connector;
    }

    private static HttpConfiguration httpConfig() {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        httpConfig.setRelativeRedirectAllowed(true);
        return httpConfig;
    }
}
