package com.druvu.web.core.handlers;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpResponse;
import com.druvu.web.api.utils.HandlerNameConvention;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/** @author Deniss Larka on 09 June 2024 */
@Slf4j
public final class HttpResponseImpl implements HttpResponse {

    private final HttpServletResponse res;

    public HttpResponseImpl(HttpServletResponse res) {
        this.res = Objects.requireNonNull(res);
    }

    @Override
    public void sendRedirect(String page) {
        res.resetBuffer();
        res.setHeader("Location", page);
        res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    public void sendRedirect(Class<? extends HttpHandler> page) {
        sendRedirect(HandlerNameConvention.translate(page));
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
