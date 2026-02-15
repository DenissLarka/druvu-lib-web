package com.druvu.web.api.handlers;

/**
 * Interface for HTTP response abstraction.
 * <p>
 * Provides methods for sending redirects, errors, and content.
 * Implementations are created via factory methods in the core module.
 *
 * @author : Deniss Larka
 * on 08 June 2024
 **/
public interface HttpResponse {

	void sendRedirect(String page);

	void sendError(int errorCode);

	boolean isCommitted();

	void commitContent(String contentType, String json);
}
