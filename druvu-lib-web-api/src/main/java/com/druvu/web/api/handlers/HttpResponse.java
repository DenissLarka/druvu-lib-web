package com.druvu.web.api.handlers;

/**
 * The response as a handler shapes it. A handler that commits nothing lets the framework render its template; one that
 * commits, redirects or sends an error has answered, and no template runs.
 *
 * <pre>{@code
 * response.commit("application/json", json);                 // 200 with a body
 * response.header("Allow", "POST").commit(405);              // a status alone, after its header
 * response.sendRedirect(AccountHandler.class);               // 302 to that handler's route
 * }</pre>
 *
 * @author Deniss Larka <br>
 *     on 08 Jun 2024
 */
public interface HttpResponse {

    /**
     * Sets a response header; before committing.
     *
     * @return this response, so the commit can follow in the same statement.
     */
    HttpResponse header(String name, String value);

    /** Commits this status with no body: 202 or 204, or 405 with an {@code Allow} header set before. */
    void commit(int status);

    /** Commits this status with this content type and body, written as UTF-8. */
    void commit(int status, String contentType, String body);

    /** Commits 200 with this content type and body, written as UTF-8. */
    void commit(String contentType, String body);

    /** Answers with a redirect to this page, through the container. */
    void sendRedirect(String page);

    /**
     * Answers with a redirect to this handler's route in this application: its path under the context path, so the
     * redirect holds from any depth of the current URL and follows the handler when it is renamed.
     *
     * @throws IllegalArgumentException when the handler is not one of the application's routes
     */
    void sendRedirect(Class<? extends HttpHandler> handler);

    /** Answers with this status through the container's error handler, which renders its error page. */
    void sendError(int status);

    /** Whether the response has been committed, by this handler or before it. */
    boolean isCommitted();
}
