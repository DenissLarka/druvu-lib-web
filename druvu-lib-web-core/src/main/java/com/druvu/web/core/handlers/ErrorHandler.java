package com.druvu.web.core.handlers;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author : Deniss Larka
 * @since : 2021 February 10
 **/
@Slf4j
public class ErrorHandler implements HttpHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response) {
		log.warn("NOT FOUND: {}", request);
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
}
