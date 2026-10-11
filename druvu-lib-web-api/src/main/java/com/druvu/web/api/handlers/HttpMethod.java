package com.druvu.web.api.handlers;

/**
 * The HTTP method of a request, typed: the nine of RFC 9110, and {@link #OTHER} for any further token a client may
 * send, {@code PROPFIND} say, which the container accepts and a handler may still want to refuse with a 405. The token
 * itself stays reachable through {@code HttpRequest.methodName()}.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public enum HttpMethod {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    PATCH,
    OPTIONS,
    TRACE,
    CONNECT,
    /** A method outside the nine; the request's {@code methodName()} has the token. */
    OTHER;

    /**
     * @param token the method as it came on the wire; methods are case-sensitive, so {@code get} is not {@link #GET}.
     * @return the method, or {@link #OTHER} for a token outside the nine.
     */
    public static HttpMethod of(String token) {
        for (HttpMethod method : values()) {
            if (method != OTHER && method.name().equals(token)) {
                return method;
            }
        }
        return OTHER;
    }
}
