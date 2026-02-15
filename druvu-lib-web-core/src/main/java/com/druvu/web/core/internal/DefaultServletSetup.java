package com.druvu.web.core.internal;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

/**
 * @author Deniss Larka
 * on 11 February 2022
 */
public class DefaultServletSetup implements Consumer<ServletContextHandler> {
	private final Set<String> staticPaths;

	public DefaultServletSetup(Set<String> staticPaths) {
		this.staticPaths = Set.copyOf(Objects.requireNonNull(staticPaths));
	}

	@Override
	public void accept(ServletContextHandler servletContext) {
		ServletHolder holder = new ServletHolder("default", DefaultServlet.class);
		holder.setInitOrder(1);
		holder.setInitParameter("cacheControl", "max-age=604800,public");
		for (String staticPath : staticPaths) {
			servletContext.addServlet(holder, staticPath);
		}
	}
}
