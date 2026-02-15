module com.druvu.lib.web.api {
	// Compile-time only
	requires static lombok;
	requires static com.github.spotbugs.annotations;
	requires static org.slf4j;

	// Runtime dependencies
	requires transitive druvu.lib.loader;
	requires transitive jakarta.servlet;


	// Export all API packages
	exports com.druvu.web.api.config;
	exports com.druvu.web.api.handlers;
	exports com.druvu.web.api.auth;
	exports com.druvu.web.api.plugin;
	exports com.druvu.web.api.utils;

	// Declare usage of plugin factories
	uses com.druvu.lib.loader.ComponentFactory;
}
