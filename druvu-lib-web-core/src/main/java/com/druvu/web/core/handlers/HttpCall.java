package com.druvu.web.core.handlers;

import java.util.Objects;

import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.druvu.web.api.handlers.PathInfo;
import com.druvu.web.api.utils.BeanNameConvention;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Deniss Larka
 * on 14 March 2022
 */
@Slf4j
public class HttpCall {

	private static final String HTTP_METHOD_POST = "POST";
	private static final String HTTP_METHOD_GET = "GET";
	private final HttpRequest req;
	private final HttpResponse res;

	public HttpCall(HttpRequest req, HttpResponse res) {
		this.req = Objects.requireNonNull(req);
		this.res = Objects.requireNonNull(res);
	}

	public void putToRequestScope(String key, Object value) {
		req.setAttribute(key, value);
	}

	public HttpRequest request() {
		return req;
	}

	public HttpResponse response() {
		return res;
	}
	public PathInfo pathInfo() {
		return req.pathInfo();
	}

	public <C> C fromApplicationScope(String key) {
		if (!req.globalAttributes().has(key)) {
			throw new IllegalStateException("No component found in the application scope:" + key);
		}
		return req.globalAttributes().get(key);
	}

	public <C> C fromApplicationScope(Class<C> key) {
		return fromApplicationScope(BeanNameConvention.keyFromClass(key));
	}

	/*
	 * =============== METHOD =====================
	 */
	public boolean isGet() {
		return HTTP_METHOD_GET.equalsIgnoreCase(request().method());
	}

	public boolean isPost() {
		return HTTP_METHOD_POST.equalsIgnoreCase(request().method());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("req=");
		builder.append(request().pathInfo());
		return builder.toString();
	}

}
