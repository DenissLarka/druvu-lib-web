package com.druvu.web.api.plugin;

/**
 * Plugin interface for template engines (JSP, PHP, etc.).
 * <p>
 * Implementations are discovered via ServiceLoader using druvu-lib-loader.
 * Each plugin registers its servlet(s) during context initialisation.
 * <p>
 * Expected to be registered in module-info.java:
 * <pre>
 * provides com.druvu.lib.loader.ComponentFactory
 *     with com.example.MyTemplateEnginePluginFactory;
 * </pre>
 *
 * @author Deniss Larka
 */
public interface TemplateEnginePlugin {

	/**
	 * Register this template engine's servlet(s) with the servlet context handler.
	 * <p>
	 * The handler parameter is an {@code org.eclipse.jetty.ee10.servlet.ServletContextHandler}
	 * passed as Object to avoid tight coupling with Jetty in the API module.
	 * Implementations should cast it appropriately.
	 *
	 * @param handler the servlet context handler (Jetty ServletContextHandler)
	 */
	void registerServlet(Object handler);

	/**
	 * @return file extensions this engine handles (e.g., ["jsp", "jspx"])
	 */
	String[] getSupportedExtensions();

	/**
	 * @return human-readable name for logging (e.g., "JSP Engine")
	 */
	String getName();

	/**
	 * @return priority for extension conflict resolution (higher = preferred)
	 */
	default int getPriority() {
		return 0;
	}
}
