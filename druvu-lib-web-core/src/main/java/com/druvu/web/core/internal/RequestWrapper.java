package com.druvu.web.core.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Objects;

/** @author Deniss Larka on 18 February 2023 */
public final class RequestWrapper extends HttpServletRequestWrapper {

    private final String pathInfo;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request the {@link HttpServletRequest} to be wrapped.
     * @param pathInfo static value always returned by {@link #getPathInfo()}
     */
    public RequestWrapper(HttpServletRequest request, String pathInfo) {
        super(request);
        this.pathInfo = Objects.requireNonNull(pathInfo);
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }
}
