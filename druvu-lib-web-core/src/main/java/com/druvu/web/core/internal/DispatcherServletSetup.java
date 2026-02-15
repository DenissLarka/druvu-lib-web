package com.druvu.web.core.internal;

import java.util.function.Consumer;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import jakarta.servlet.MultipartConfigElement;

/**
 * @author Deniss Larka
 * on 11 February 2022
 */
public class DispatcherServletSetup implements Consumer<ServletContextHandler> {

	@Override
	public void accept(ServletContextHandler servletContext) {
		ServletHolder holder = new ServletHolder("dispatcher", DispatcherServlet.class);
		holder.setInitOrder(0);
		holder.setAsyncSupported(false);
		holder.getRegistration().setMultipartConfig(new MultipartConfigElement(System.getProperty("java.io.tmpdir")));
		servletContext.addServlet(holder, "/*");
	}
}
