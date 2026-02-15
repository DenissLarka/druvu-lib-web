package com.druvu.web.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;

import com.druvu.lib.loader.MultiComponentLoader;
import com.druvu.web.api.config.WebConfig;
import com.druvu.web.api.plugin.TemplateEnginePlugin;
import com.druvu.web.core.internal.ws.WebSocketSetup;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author : Deniss Larka
 * <br/>on 21 Apr 2024
 **/
@Slf4j
public class ContextCreator {

	public static ServletContextHandler create(WebConfig webConfig, String contextPath) {
		ServletContextHandler webappContext = new ServletContextHandler(contextPath, ServletContextHandler.NO_SESSIONS);
		ResourcesSetup resourcesSetup = new ResourcesSetup(webConfig.serveFromDirectory());
		webappContext.setBaseResource(resourcesSetup.call());
		webappContext.setErrorHandler(new CustomErrorHandler());

		// Core servlets
		List<Consumer<ServletContextHandler>> setups = new ArrayList<>();
		setups.add(new DispatcherServletSetup());
		setups.add(new DefaultServletSetup(webConfig.staticPaths()));
		setups.add(new WebSocketSetup());

		// Discover and register template engine plugins
		List<TemplateEnginePlugin> plugins = MultiComponentLoader.loadAll(TemplateEnginePlugin.class);
		for (TemplateEnginePlugin plugin : plugins) {
			log.info("Registering template engine: {}", plugin.getName());
			setups.add(plugin::registerServlet);
		}

		setups.forEach(consumer -> consumer.accept(webappContext));
		return webappContext;
	}
}
