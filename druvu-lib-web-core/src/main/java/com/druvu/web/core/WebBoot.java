package com.druvu.web.core;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.api.config.WebConfig;
import com.druvu.web.core.auth.JettyAuthAdapter;
import com.druvu.web.core.internal.ConnectorsInstaller;
import com.druvu.web.core.internal.ContextCreator;
import com.druvu.web.core.internal.ContextVars;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 *
 * @author : Deniss Larka
 * <br/>on 21 Apr 2024
 **/
@AllArgsConstructor
public class WebBoot {

	@NonNull
	private final WebConfig webConfig;

	@SneakyThrows
	public LifeCycle start(String contextPath) {
		if (contextPath == null || contextPath.isEmpty()) {
			throw new IllegalArgumentException("contextPath is null or empty");
		}
		if (contextPath.charAt(0) != '/') {
			throw new IllegalArgumentException("contextPath must start with '/'");
		}
		Server server = new Server();
		server.setStopAtShutdown(true);
		ConnectorsInstaller.install(webConfig, server);
		final ServletContextHandler handler = ContextCreator.create(webConfig, contextPath);
		handler.addEventListener(new Listener());
		server.setHandler(handler);
		server.start();
		return server;
	}

	private class Listener implements ServletContextListener {

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			final ServletContext context = sce.getServletContext();
			setUpAppScopeBeans(context);
			context.setAttribute(ContextVars.TEMPLATE_SYSTEM, webConfig.templateSystem());
			context.setAttribute(ContextVars.HANDLERS, urlConfigsAsMap());
			context.setAttribute(ContextVars.HTTP_DEFAULT, webConfig.urlConfigs().getFirst().url());

			final AuthConfig authConfig = webConfig.authConfig();
			if (authConfig == null) {
				throw new IllegalStateException("AuthConfig must be provided via WebConfig.builder().authConfig(...)");
			}
			context.setAttribute(ContextVars.AUTH_CONFIG, authConfig);
			context.setAttribute(ContextVars.JETTY_AUTH, new JettyAuthAdapter(authConfig));
			context.setAttribute(ContextVars.WS_OPENED, Collections.newSetFromMap(new ConcurrentHashMap<>()));
		}

		private Map<String, UrlConfig> urlConfigsAsMap() {
			return webConfig.urlConfigs().stream().collect(Collectors.toMap(UrlConfig::url, Function.identity()));
		}

		private void setUpAppScopeBeans(ServletContext context) {
			for (Map.Entry<String, Object> entry : webConfig.globalObjects().entrySet()) {
				context.setAttribute(entry.getKey(), entry.getValue());
			}
		}
	}

}
