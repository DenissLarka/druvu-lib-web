package com.druvu.web.core.internal.ws;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.http.DateGenerator;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Deniss Larka
 * on 22 September 2023
 */
public class WebSocketHttpServletResponseDelegate implements HttpServletResponse {

	public static final String RESPONSE_UPGRADED = "Response upgraded";

	private final JettyServerUpgradeResponse upResponse;

	public WebSocketHttpServletResponseDelegate(JettyServerUpgradeResponse upResponse) {
		this.upResponse = Objects.requireNonNull(upResponse);
	}

	@Override
	public void addCookie(Cookie cookie) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public boolean containsHeader(String name) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public String encodeURL(String url) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public String encodeRedirectURL(String url) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void sendError(int statusCode, String message) throws IOException {
		upResponse.sendError(statusCode, message);
	}

	@Override
	public void sendError(int statusCode) throws IOException {
		upResponse.sendError(statusCode, "");
	}

	@Override
	public void sendRedirect(String location) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void setDateHeader(String name, long date) {
		upResponse.setHeader(name, DateGenerator.formatDate(date));
	}

	@Override
	public void addDateHeader(String name, long date) {
		upResponse.addHeader(name, DateGenerator.formatDate(date));
	}

	@Override
	public void setHeader(String name, String value) {
		upResponse.setHeader(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		upResponse.addHeader(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void addIntHeader(String name, int value) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void setStatus(int statusCode) {
		upResponse.setStatusCode(statusCode);
	}

	@Override
	public int getStatus() {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public String getHeader(String name) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public Collection<String> getHeaderNames() {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public String getCharacterEncoding() {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public String getContentType() {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void setCharacterEncoding(String charset) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void setContentLength(int len) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void setContentLengthLong(long len) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void setContentType(String type) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void setBufferSize(int size) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public int getBufferSize() {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void flushBuffer() throws IOException {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void resetBuffer() {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public boolean isCommitted() {
		return upResponse.isCommitted();
	}

	@Override
	public void reset() {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public void setLocale(Locale loc) {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}

	@Override
	public Locale getLocale() {
		throw new IllegalStateException(RESPONSE_UPGRADED);
	}
}
