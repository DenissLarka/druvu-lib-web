module druvu.lib.web.core {
	// Compile-time only
	requires static lombok;
	requires static com.github.spotbugs.annotations;

	// API module (transitive for users)
	requires transitive druvu.lib.web.api;

	// Runtime dependencies
	requires druvu.lib.loader;
	requires org.eclipse.jetty.ee10.servlet;
	requires org.eclipse.jetty.ee10.websocket.jetty.server;
	requires org.eclipse.jetty.http;
	requires org.eclipse.jetty.security;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.util;
	requires org.eclipse.jetty.websocket.api;
	requires com.google.gson;
	requires org.slf4j;

	// Export public packages
	exports com.druvu.web.core;
	exports com.druvu.web.core.auth;
	exports com.druvu.web.core.handlers;
	exports com.druvu.web.core.handlers.attr;
	exports com.druvu.web.core.utils;

	// Open for reflection (Jetty needs this)
	opens com.druvu.web.core.internal to org.eclipse.jetty.ee10.servlet;

	// Use plugin factories
	uses com.druvu.lib.loader.ComponentFactory;
}
