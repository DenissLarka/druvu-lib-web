package com.druvu.web.php.internal;

import java.util.function.Consumer;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

/**
 * Sets up the PHP servlet to handle PHP files with echo tag support.
 *
 * @author Deniss Larka
 */
public class PhpServletSetup implements Consumer<ServletContextHandler> {

	@Override
	public void accept(ServletContextHandler servletContext) {
		// Create and register PHP Servlet (must be named "php" to match DispatcherServlet)
		ServletHolder holderPhp = new ServletHolder("php", PhpServlet.class);

		// Add servlet to handler without direct mapping
		// PHP files will be served only through the dispatcher
		servletContext.getServletHandler().addServlet(holderPhp);
	}
}
