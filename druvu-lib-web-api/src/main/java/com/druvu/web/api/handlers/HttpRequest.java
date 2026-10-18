package com.druvu.web.api.handlers;

import com.druvu.web.api.auth.AuthUserIdentity;
import jakarta.servlet.DispatcherType;
import java.util.List;
import java.util.Optional;

/**
 * The request as a handler sees it: the path and the parameters typed, the body and the headers as text, the signed-in
 * user if any, and the attributes the handler sets for its template. The core module creates it.
 *
 * @author Deniss Larka <br>
 *     on 08 Jun 2024
 */
public interface HttpRequest {

    /** Sets a request attribute; one whose name is a legal PHP variable name reaches the template as {@code $name}. */
    void setAttribute(String key, Object value);

    Object getAttribute(String attributeName);

    /** The application's global objects and routes. */
    GlobalAttributes globalAttributes();

    /** The URL's segments, typed. */
    PathInfo pathInfo();

    /** Query and form parameters, typed; of a multipart form, the text fields: its files are {@link #uploads()}. */
    ParamInfo paramInfo();

    /**
     * The file sent under this form field in a {@code multipart/form-data} body, or empty when none came; the first
     * when several came under one name.
     */
    Optional<UploadedFile> upload(String fieldName);

    /** Every file of a {@code multipart/form-data} body, in the order sent; empty for any other request. */
    List<UploadedFile> uploads();

    /**
     * The HTTP method, typed; {@link HttpMethod#OTHER} for a token outside the nine, which {@link #methodName()} has.
     */
    HttpMethod method();

    /** The method as it came on the wire, for an {@code Allow} header or a log line. */
    String methodName();

    /** Whether this is a GET: the page is shown. */
    default boolean isGet() {
        return method() == HttpMethod.GET;
    }

    /** Whether this is a POST: a form or a body was submitted. */
    default boolean isPost() {
        return method() == HttpMethod.POST;
    }

    /** The request's content type, or null when it carries none. */
    String contentType();

    /** A request header, or empty when absent; the first value when there are several. */
    Optional<String> header(String name);

    /**
     * The request body as text, decoded with the request's charset or UTF-8; empty when there is none. Read once and
     * kept, so asking twice is fine. A form body, URL-encoded or multipart, has already become {@link #paramInfo()} and
     * reads as empty here.
     */
    String body();

    /** The first URL segment, which names the handler. */
    String mainPath();

    DispatcherType dispatcherType();

    boolean isDispatcherTypeRequest();

    boolean isDispatcherTypeInclude();

    /** The signed-in user, or empty on a public route or without an {@code AuthConfig}. */
    Optional<AuthUserIdentity> user();
}
