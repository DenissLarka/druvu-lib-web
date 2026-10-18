package com.druvu.web.core.handlers;

import com.druvu.web.api.config.Route;
import com.druvu.web.api.handlers.GlobalAttributes;
import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * The servlet response behind {@link HttpResponse}. It also knows the context path and the application's routes, which
 * a redirect to a handler needs to build that handler's path.
 *
 * @author Deniss Larka on 09 June 2024
 */
@Slf4j
public final class HttpResponseImpl implements HttpResponse {

    private final HttpServletResponse res;
    private final String contextPath;
    private final GlobalAttributes globalAttributes;

    public HttpResponseImpl(HttpServletResponse res, String contextPath, GlobalAttributes globalAttributes) {
        this.res = Objects.requireNonNull(res);
        this.contextPath = Objects.requireNonNull(contextPath, "contextPath");
        this.globalAttributes = Objects.requireNonNull(globalAttributes, "globalAttributes");
    }

    /** The container's redirect commits the response, so the dispatcher does not go on to a template after it. */
    @SneakyThrows
    @Override
    public void sendRedirect(String page) {
        res.sendRedirect(page);
    }

    /** The path comes from the registered route, not from the class name, so it is the URL the route really answers. */
    @Override
    public void sendRedirect(Class<? extends HttpHandler> handler) {
        Objects.requireNonNull(handler, "handler");
        Route<?> route = globalAttributes.routes().values().stream()
                .filter(candidate -> handler.equals(candidate.handlerClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No route for " + handler.getName() + ": a redirect goes to one of the application's routes"));
        sendRedirect(contextPath + "/" + route.url());
    }

    @SneakyThrows
    @Override
    public void sendError(int status) {
        res.sendError(status);
    }

    @Override
    public boolean isCommitted() {
        return res.isCommitted();
    }

    @Override
    public HttpResponse header(String name, String value) {
        res.setHeader(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
        return this;
    }

    @Override
    public void commit(String contentType, String body) {
        commit(HttpServletResponse.SC_OK, contentType, body);
    }

    @Override
    public void commit(int status, String contentType, String body) {
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(body, "body");
        res.setStatus(status);
        // Bytes and declaration agree: the servlet default would be ISO-8859-1 and mangle any non-ASCII text.
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType(contentType);
        try {
            res.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            res.flushBuffer();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void commit(int status) {
        res.setStatus(status);
        res.setContentLength(0);
        try {
            res.flushBuffer();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
