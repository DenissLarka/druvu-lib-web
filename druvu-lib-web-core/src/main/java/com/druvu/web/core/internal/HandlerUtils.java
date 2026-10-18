package com.druvu.web.core.internal;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.api.config.Route;
import com.druvu.web.api.config.UrlHandler;
import com.druvu.web.api.handlers.GlobalAttributes;
import com.druvu.web.api.handlers.HttpCall;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.druvu.web.api.handlers.PathInfo;
import com.druvu.web.core.handlers.HttpRequestImpl;
import com.druvu.web.core.handlers.HttpResponseImpl;
import com.druvu.web.core.handlers.attr.GlobalAttributesImpl;
import com.druvu.web.core.security.Identities;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;

/**
 * Turns a request into an {@link HttpCall} for its route, or answers it when it may not go further.
 *
 * <p>Jetty's security handler has already run: an anonymous visitor to a protected route was challenged before this
 * code saw the request. What is decided here is authorisation, because a route's permissions are all required and
 * Jetty's role constraints grant on any one of them: a signed-in user lacking a permission gets 403.
 *
 * @author Deniss Larka <br>
 *     on 20 May 2024
 */
public class HandlerUtils {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    public static Optional<HttpCall> process(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return doProcess(httpServletRequest, httpServletResponse);
    }

    private static Optional<HttpCall> doProcess(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        final GlobalAttributes globalAttributes = GlobalAttributesImpl.from(httpServletRequest.getServletContext());
        final String mainPath = resolveMainPath(httpServletRequest, globalAttributes);
        final Route<?> route = globalAttributes.routes().get(mainPath);
        final Optional<AuthConfig> auth = ContextVars.authConfig(httpServletRequest.getServletContext());
        final Optional<AuthUserIdentity> user = auth.flatMap(config -> Identities.of(httpServletRequest, config));
        if (route != null && route.requiresSignIn()) {
            if (user.isEmpty()) {
                return refused(httpServletResponse, HttpServletResponse.SC_UNAUTHORIZED);
            }
            if (!user.get().getPermissions().containsAll(route.permissions())) {
                return refused(httpServletResponse, HttpServletResponse.SC_FORBIDDEN);
            }
        }
        if (isOverUploadLimit(httpServletRequest)) {
            return refused(httpServletResponse, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        }
        HttpRequest req = new HttpRequestImpl(httpServletRequest, user);
        HttpResponse resp =
                new HttpResponseImpl(httpServletResponse, httpServletRequest.getContextPath(), globalAttributes);
        return Optional.of(new HttpCall(req, resp));
    }

    /** A multipart body that declares more bytes than the upload limit is refused before any of it is read. */
    private static boolean isOverUploadLimit(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.regionMatches(true, 0, MULTIPART_FORM_DATA, 0, MULTIPART_FORM_DATA.length())
                && request.getContentLengthLong() > ContextVars.uploadLimit(request.getServletContext());
    }

    @SneakyThrows
    private static Optional<HttpCall> refused(HttpServletResponse response, int status) {
        if (!response.isCommitted()) {
            response.sendError(status);
        }
        return Optional.empty();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <H extends UrlHandler> H handler(GlobalAttributes context, String matchPath) {
        final Route<?> route = Objects.requireNonNull(context.routes().get(matchPath), () -> "No route " + matchPath);
        return (H) route.handlerClass().getDeclaredConstructor().newInstance();
    }

    private static String resolveMainPath(HttpServletRequest request, GlobalAttributes globalAttributes) {
        PathInfo pathInfo =
                new PathInfo(request.getContextPath(), request.getPathInfo(), globalAttributes.defaultPath());
        return pathInfo.mainPath();
    }
}
