package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.value.PhpArray;
import com.druvu.web.php.internal.value.PhpString;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * PHP's request arrays, filled from the servlet request.
 *
 * <p>Read-only in the way that matters: they are ordinary arrays holding copies, so a template can read and even
 * rewrite its own copy without any of it reaching the request.
 *
 * <p>{@code $_SESSION}, {@code $_FILES}, {@code $_ENV} and {@code $GLOBALS} are absent and are meant to be. Sessions
 * and uploads are the application's business, and a template that reads the environment is a template that can leak it.
 *
 * @author Deniss Larka
 */
public final class Superglobals {

    private Superglobals() {}

    /** Binds {@code $_GET}, {@code $_POST}, {@code $_REQUEST}, {@code $_COOKIE} and {@code $_SERVER}. */
    public static void bindInto(Env env, HttpServletRequest request) {
        if (request == null) {
            return;
        }
        PhpArray get = fromQueryString(request.getQueryString());
        PhpArray all = fromParameters(request.getParameterMap());

        env.setVariable("_GET", get);
        env.setVariable("_POST", postOnly(all, get));
        env.setVariable("_REQUEST", all);
        env.setVariable("_COOKIE", fromCookies(request));
        env.setVariable("_SERVER", fromRequest(request));
    }

    /** Everything the container parsed, which is the query string and the form body together. */
    private static PhpArray fromParameters(Map<String, String[]> parameters) {
        PhpArray array = PhpArray.empty();
        parameters.forEach((name, values) -> array.put(
                com.druvu.web.php.internal.value.ArrayKey.of(name),
                values.length == 0 ? PhpString.of("") : PhpString.of(values[values.length - 1])));
        return array;
    }

    private static PhpArray fromQueryString(String queryString) {
        PhpArray array = PhpArray.empty();
        if (queryString == null || queryString.isEmpty()) {
            return array;
        }
        for (String pair : queryString.split("&")) {
            int equals = pair.indexOf('=');
            String name = decode(equals < 0 ? pair : pair.substring(0, equals));
            String value = equals < 0 ? "" : decode(pair.substring(equals + 1));
            array.put(com.druvu.web.php.internal.value.ArrayKey.of(name), PhpString.of(value));
        }
        return array;
    }

    /** What was posted is what the container parsed minus what was in the query string. */
    private static PhpArray postOnly(PhpArray all, PhpArray get) {
        PhpArray posted = PhpArray.empty();
        all.entries().forEach((key, value) -> {
            if (!get.containsKey(key)) {
                posted.put(key, value);
            }
        });
        return posted;
    }

    private static PhpArray fromCookies(HttpServletRequest request) {
        PhpArray array = PhpArray.empty();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return array;
        }
        for (Cookie cookie : cookies) {
            array.put(com.druvu.web.php.internal.value.ArrayKey.of(cookie.getName()), PhpString.of(cookie.getValue()));
        }
        return array;
    }

    private static PhpArray fromRequest(HttpServletRequest request) {
        PhpArray array = PhpArray.empty();
        put(array, "REQUEST_METHOD", request.getMethod());
        put(array, "REQUEST_URI", request.getRequestURI());
        put(array, "QUERY_STRING", request.getQueryString());
        put(array, "SCRIPT_NAME", request.getServletPath());
        put(array, "PATH_INFO", request.getPathInfo());
        put(array, "SERVER_NAME", request.getServerName());
        put(array, "SERVER_PORT", String.valueOf(request.getServerPort()));
        put(array, "SERVER_PROTOCOL", request.getProtocol());
        put(array, "REMOTE_ADDR", request.getRemoteAddr());
        put(array, "HTTP_HOST", request.getHeader("Host"));
        put(array, "HTTP_USER_AGENT", request.getHeader("User-Agent"));
        put(array, "HTTP_REFERER", request.getHeader("Referer"));
        put(array, "HTTPS", request.isSecure() ? "on" : null);
        return array;
    }

    private static void put(PhpArray array, String name, String value) {
        array.put(com.druvu.web.php.internal.value.ArrayKey.of(name), PhpString.of(value == null ? "" : value));
    }

    private static String decode(String text) {
        return java.net.URLDecoder.decode(text, java.nio.charset.StandardCharsets.UTF_8);
    }
}
