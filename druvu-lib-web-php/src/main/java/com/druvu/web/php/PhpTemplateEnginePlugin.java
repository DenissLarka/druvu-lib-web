package com.druvu.web.php;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;

import com.druvu.web.api.plugin.TemplateEnginePlugin;
import com.druvu.web.php.internal.PhpServletSetup;

/**
 * PHP template engine plugin implementation.
 * Registers PHP servlet with the servlet context.
 *
 * @author Deniss Larka
 */
public class PhpTemplateEnginePlugin implements TemplateEnginePlugin {

	@Override
	public void registerServlet(Object handler) {
		if (handler instanceof ServletContextHandler servletContextHandler) {
			new PhpServletSetup().accept(servletContextHandler);
		} else {
			throw new IllegalArgumentException("Expected ServletContextHandler, got: " + handler.getClass());
		}
	}

	@Override
	public String[] getSupportedExtensions() {
		return new String[]{"php"};
	}

	@Override
	public String getName() {
		return "PHP Engine";
	}

	@Override
	public int getPriority() {
		return 50;
	}
}
