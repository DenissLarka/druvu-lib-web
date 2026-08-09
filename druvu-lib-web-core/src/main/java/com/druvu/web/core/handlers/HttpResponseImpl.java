package com.druvu.web.core.handlers;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpResponse;
import com.druvu.web.api.utils.HandlerNameConvention;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpHeader;

/** @author : Deniss Larka on 09 June 2024 */
@Slf4j
public final class HttpResponseImpl implements HttpResponse {

    private final HttpServletResponse res;

    public HttpResponseImpl(HttpServletResponse res) {
        this.res = Objects.requireNonNull(res);
    }

    @Override
    public void sendRedirect(String page) {
        res.resetBuffer();
        res.setHeader(HttpHeader.LOCATION.asString(), page);
        res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    public void sendRedirect(Class<? extends HttpHandler> page) {
        sendRedirect(HandlerNameConvention.translate(page));
    }

    @SneakyThrows
    @Override
    public void sendError(int errorCode) {
        res.sendError(errorCode);
    }

    @Override
    public boolean isCommitted() {
        return res.isCommitted();
    }

    @Override
    public void commitContent(String contentType, String content) {
        res.setContentType(Objects.requireNonNull(contentType));
        try {
            res.getOutputStream().print(Objects.requireNonNull(content));
            res.flushBuffer();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
