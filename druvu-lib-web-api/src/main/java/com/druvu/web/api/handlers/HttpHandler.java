package com.druvu.web.api.handlers;

import com.druvu.web.api.config.UrlHandler;

/**
 * @author : Deniss Larka
 * on 10 February 2021
 **/
public interface HttpHandler extends UrlHandler {

	default void handle(HttpRequest request, HttpResponse response) {

	}
}
