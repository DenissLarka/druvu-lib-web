package com.druvu.web.php.internal;

import com.druvu.php.HostRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A servlet request, read the way the engine wants it.
 *
 * <p>{@code $_GET} is the query string parsed by hand, because the container's parameter map merges the query and the
 * body and PHP keeps them apart; {@code $_POST} is then what the container parsed minus what the query string already
 * held. Where a name arrives more than once, the last value wins, as in PHP.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public final class ServletHostRequest implements HostRequest {

    private final HttpServletRequest request;

    public ServletHostRequest(HttpServletRequest request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public Map<String, String> queryParameters() {
        Map<String, String> query = new LinkedHashMap<>();
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return query;
        }
        for (String pair : queryString.split("&")) {
            int equals = pair.indexOf('=');
            String name = decode(equals < 0 ? pair : pair.substring(0, equals));
            String value = equals < 0 ? "" : decode(pair.substring(equals + 1));
            query.put(name, value);
        }
        return query;
    }

    @Override
    public Map<String, String> formParameters() {
        Map<String, String> query = queryParameters();
        Map<String, String> form = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            if (!query.containsKey(name)) {
                form.put(name, values.length == 0 ? "" : values[values.length - 1]);
            }
        });
        return form;
    }

    @Override
    public Map<String, String> cookies() {
        Map<String, String> cookies = new LinkedHashMap<>();
        Cookie[] all = request.getCookies();
        if (all != null) {
            for (Cookie cookie : all) {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }
        return cookies;
    }

    @Override
    public Map<String, String> serverVariables() {
        Map<String, String> server = new LinkedHashMap<>();
        put(server, "REQUEST_METHOD", request.getMethod());
        put(server, "REQUEST_URI", request.getRequestURI());
        put(server, "QUERY_STRING", request.getQueryString());
        put(server, "SCRIPT_NAME", request.getServletPath());
        put(server, "PATH_INFO", request.getPathInfo());
        put(server, "SERVER_NAME", request.getServerName());
        put(server, "SERVER_PORT", String.valueOf(request.getServerPort()));
        put(server, "SERVER_PROTOCOL", request.getProtocol());
        put(server, "REMOTE_ADDR", request.getRemoteAddr());
        put(server, "REMOTE_USER", request.getRemoteUser());
        put(server, "HTTP_HOST", request.getHeader("Host"));
        put(server, "HTTP_USER_AGENT", request.getHeader("User-Agent"));
        put(server, "HTTP_REFERER", request.getHeader("Referer"));
        put(server, "HTTPS", request.isSecure() ? "on" : null);
        return server;
    }

    @Override
    public String contextPath() {
        return request.getContextPath();
    }

    private static void put(Map<String, String> map, String name, String value) {
        map.put(name, value == null ? "" : value);
    }

    private static String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }
}
