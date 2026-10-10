package com.druvu.web.core.test.wiring;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;

/**
 * A fake OpenID Connect provider for the redirect flow: discovery, an authorize endpoint that signs everyone in as one
 * fixed user at once, and a token endpoint that mints an unsigned id_token, which is all Jetty's decoder reads.
 */
final class FakeOpenIdProvider implements AutoCloseable {
    static final String CLIENT_ID = "test-client";
    static final String CLIENT_SECRET = "test-secret";
    static final String SUBJECT = "alice-sub";
    static final String EMAIL = "alice@example.com";
    static final String NAME = "Alice";
    static final String CODE = "fake-code";

    private static final String LOOPBACK = InetAddress.getLoopbackAddress().getHostAddress();

    private final Server server = new Server();
    private final ServerConnector connector;

    FakeOpenIdProvider() {
        connector = new ServerConnector(server);
        connector.setHost(LOOPBACK);
        connector.setPort(0);
        server.addConnector(connector);
        server.setHandler(new Endpoints());
        try {
            server.start();
        } catch (Exception failed) {
            throw new IllegalStateException("the fake provider did not start", failed);
        }
    }

    String issuer() {
        return "http://" + LOOPBACK + ":" + connector.getLocalPort();
    }

    @Override
    public void close() throws Exception {
        server.stop();
    }

    private final class Endpoints extends Handler.Abstract {
        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            switch (Request.getPathInContext(request)) {
                case "/.well-known/openid-configuration" -> json(response, callback, discovery());
                case "/authorize" -> authorize(request, response, callback);
                case "/token" -> json(response, callback, tokens());
                default -> Response.writeError(request, response, callback, 404);
            }
            return true;
        }

        private void authorize(Request request, Response response, Callback callback) {
            Fields query = Request.extractQueryParameters(request);
            String back = query.getValue("redirect_uri") + "?code=" + CODE + "&state="
                    + URLEncoder.encode(query.getValue("state"), StandardCharsets.UTF_8);
            Response.sendRedirect(request, response, callback, back);
        }
    }

    private String discovery() {
        return "{\"issuer\":\"" + issuer() + "\",\"authorization_endpoint\":\"" + issuer()
                + "/authorize\",\"token_endpoint\":\"" + issuer() + "/token\"}";
    }

    private String tokens() {
        long now = Instant.now().getEpochSecond();
        String claims = "{\"iss\":\"" + issuer() + "\",\"sub\":\"" + SUBJECT + "\",\"aud\":\"" + CLIENT_ID
                + "\",\"exp\":" + (now + 3600) + ",\"iat\":" + now + ",\"email\":\"" + EMAIL + "\",\"name\":\"" + NAME
                + "\"}";
        String idToken =
                base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + base64Url(claims) + "." + base64Url("unsigned");
        return "{\"access_token\":\"fake-access\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"id_token\":\""
                + idToken + "\"}";
    }

    private static String base64Url(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void json(Response response, Callback callback, String body) {
        response.setStatus(200);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json");
        response.write(true, ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)), callback);
    }
}
