package com.druvu.web.core.internal;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

import org.eclipse.jetty.ee10.servlet.ServletContextRequest;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

public class CustomErrorHandler extends ErrorHandler {

	@Override
	protected void writeErrorHtml(Request request, Writer writer, Charset charset, int code, String message, Throwable cause) throws IOException {

		StringBuilder builder = new StringBuilder();
		builder.append("<!DOCTYPE html>\n");
		builder.append("<html>\n");
		builder.append("<body>\n");
		builder.append("<h3>\n");
		builder.append(code);
		builder.append(' ');
		builder.append(message);
		builder.append("</h3>\n");
		builder.append("<h6>\n");
		builder.append(toString(request));
		builder.append("</h6>\n");
		builder.append("</body>\n");
		builder.append("</html>\n");
		writer.write(builder.toString());
	}

	private String toString(Request request) {
		if (request instanceof ServletContextRequest request1) {
			return request1.getDecodedPathInContext();
		}
		return "";
	}
}
